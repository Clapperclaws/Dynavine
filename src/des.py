import argparse
import heapq
import networkx as nx
import os
import re
import subprocess


class Event:
    def __init__(self, ts, etype, vn_id):
        self.ts = ts
        self.etype = etype
        self.vn_id = vn_id

    def __lt__(self, e):
        return self.ts < e.ts

    def debug_string(self):
        return "ts = " + str(self.ts) + ", etype = " + self.etype + ", vn_id = " + self.vn_id

def execute_one_experiment(executable, otn_topology_file, ip_topology_file,
                           ip_node_mapping_file, ip_link_mapping_file, 
                           ip_port_info_file, vn_topology_file, 
                           vn_location_file, num_shuffles):

    command_str = executable + " " + "--otn_topology_file=" + \
                                otn_topology_file + " " + \
                                '--ip_topology_file='+ip_topology_file + " " + \
                                '--ip_node_mapping_file='+ip_node_mapping_file + \
                                " " + '--ip_port_info_file='+ip_port_info_file + \
                                " " + '--vn_topology_file='+vn_topology_file + \
                                " " + '--num_shuffles=' + str(num_shuffles)
    print command_str
    process = subprocess.Popen([executable, "--otn_topology_file=" +
                                otn_topology_file,
                                '--ip_topology_file='+ip_topology_file,
                                '--ip_node_mapping_file='+ip_node_mapping_file,
                                '--ip_link_mapping_file='+ip_link_mapping_file,
                                '--ip_port_info_file='+ip_port_info_file,
                                '--vn_topology_file='+vn_topology_file,
                                '--vn_location_file='+vn_location_file,
                                '--num_shuffles='+str(num_shuffles)], 
                               stdout = subprocess.PIPE, 
                               stderr = subprocess.PIPE, 
                               shell = False)
    for line in process.stdout:
        print line.strip("\r\n")
    for line in process.stderr:
        print line.strip("\r\n")

def load_csv_graph(topology_file):
    g = nx.MultiGraph()
    with open(topology_file, "r") as f:
        first_line = True
        for line in f:
            tokens = line.split(",")
            if first_line:
                first_line = False
                continue
            u, v, c, bandwidth = int(tokens[0]), int(tokens[1]), int(tokens[2]), int(tokens[3])
            edge_data = g.get_edge_data(u, v)
            if edge_data == None:
                g.add_edge(u, v, bw=int(bandwidth), cost=int(c), order=0)
            else:
                max_order = 0
                for idx in edge_data.keys():
                    if max_order < edge_data[idx]['order']:
                        max_order = edge_data[idx]['order']
                g.add_edge(u, v, bw=int(bandwidth), cost=int(c), order=max_order + 1)
    return g

def write_csv_graph(g, topology_file):
    with open(topology_file, "w") as f:
        f.write(str(g.number_of_nodes()) + "\n")
        edges = g.edges()
        for edge in edges:
            edge_data = g.get_edge_data(edge[0], edge[1])
            for idx in edge_data.keys():
                cost = edge_data[idx]['cost']
                bw = edge_data[idx]['bw']
                f.write(",".join([str(edge[0]), str(edge[1]), str(cost), str(bw)]) + "\n")

def write_ip_util_matrix(g, ip_util_matrix, out_file):
    with open(out_file, "w") as f:
        for (key, value) in ip_util_matrix.iteritems():
            edge_data = g[key[0]][key[1]]
            for (k, v) in edge_data.iteritems():
                if v['order'] == key[2]:
                    util = float(value / (value + float(v['bw'])))
                    if util > 0:
                        f.write(",".join([str(key[0]), str(key[1]), str(key[2]), str(util)]) + "\n")
                    break

def write_otn_util_matrix(g, otn_util_matrix, out_file):
    with open(out_file, "w") as f:
        for (key, value) in otn_util_matrix.iteritems():
            util = float(value / (value + float(g.get_edge_data(key[0], key[1])[0]['bw'])))
            if util > 0:
                f.write(",".join([str(key[0]), str(key[1]), str(util)]) + "\n")

def update_ip_topology(ip, new_ip_map_file, ip_util_matrix, port_capacities, num_ports):
    seen = set()
    new_ip_links = []
    with open(new_ip_map_file, "r") as f:
        for line in f:
            tokens = line.split(",")
            u, v, order, p, q = int(tokens[0]), int(tokens[1]), int(tokens[2]), int(tokens[3]), int(tokens[4])
            if u > v:
                u, v = v, u
            link = (u, v, order)
            if link not in seen:
                seen.add(link)
                bw = min(port_capacities[u], port_capacities[v])
                num_ports[u] -= 1
                num_ports[v] -= 1
                cost = 1
                ip.add_edge(u, v, bw=bw, cost=cost, order=order)
                ip_util_matrix[(u, v, order)] = 0
    return ip, port_capacities, num_ports

def write_ip_port_info(ip, num_ports, port_caps, ip_port_info_file):
    with open(ip_port_info_file, "w") as f:
        for i in range(0, ip.number_of_nodes()):
            f.write(",".join([str(i), str(num_ports[i]), str(port_caps[i])]))

def update_ip_capacity(ip, vn, ip_util_matrix, emap_file, increase = True):
    sign = 1
    if not increase:
        sign = -1
    with open(emap_file, "r") as f:
        for line in f:
            tokens = line.split(",")
            m, n, u, v, order = int(tokens[0]), int(tokens[1]), int(tokens[2]), int(tokens[3]), int(tokens[4])
            if m > n:
                m, n = n, m
            if u > v:
                u, v = v, u
            b_mn = int(vn.get_edge_data(m, n)[0]['bw'])
            for idx in ip.edge[u][v].keys():
                if ip.edge[u][v][idx]['order'] == order:
                    ip.edge[u][v][idx]['bw'] += (sign * b_mn)
                    ip_util_matrix[(u, v, order)] += (sign * b_mn)
    return ip

def update_otn_capacity(otn, ip, otn_util_matrix, new_ip_map_file):
    with open(new_ip_map_file, 'r') as f:
        for line in f:
            tokens = line.split(",")
            u, v, order, p, q = int(tokens[0]), int(tokens[1]), int(tokens[2]), int(tokens[3]), int(tokens[4])
            if u > v:
                u, v = v, u
            if p > q:
                p, q = q, p
            for idx in ip.edge[u][v].keys():
                if ip.edge[u][v][idx]['order'] == order:
                    otn[p][q][0]['bw'] -= ip.edge[u][v][idx]['bw']
                    otn_util_matrix[(p, q)] += ip.edge[u][v][idx]['bw']
                    break
    return otn

def get_embedding_status(status_file):
    ret = ''
    try:
        with open(status_file) as f:
            ret = f.readline().strip("\n\r")
    except IOError:
        pass
    return ret

def get_full_path(cur_directory, file_name):
    if not file_name.startswith("/"):
        return os.path.join(cur_directory, file_name)
    return file_name

def main():
    parser = argparse.ArgumentParser(
                formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument("--executable", type = str, required = True)
    parser.add_argument("--otn_topology_file", type = str, required = True)
    parser.add_argument("--ip_topology_file", type = str, required = True)
    parser.add_argument("--ip_node_mapping_file", type = str, required = True)
    parser.add_argument("--ip_link_mapping_file", type = str, required = True)
    parser.add_argument("--ip_port_info_file", type = str, required = True)
    parser.add_argument("--vnr_directory", type = str, default = "vns")
    parser.add_argument("--simulation_plan", type = str, 
                        default = "simulation-plan")
    parser.add_argument("--max_simulation_time", type = int, default = 1000)
    args = parser.parse_args()
    port_capacities = []
    num_ports = []
    with open(args.ip_port_info_file) as f:
        for line in f:
            tokens = line.split(",")
            num_ports.append(int(tokens[1]))
            port_capacities.append(int(tokens[2]))

    current_time = 0
    event_queue = []
    current_directory = os.getcwd()
    active_vns = []
    with open(args.simulation_plan, "r") as f:
        for line in f:
            tokens = line.split(",")
            ts = int(tokens[0])
            end_time = int(tokens[1])
            vn_id = tokens[2].rstrip("\n")
            e = Event(ts, "arrival", vn_id)
            heapq.heappush(event_queue, e)
            if end_time <= args.max_simulation_time:
                e = Event(end_time, "departure", vn_id)
                heapq.heappush(event_queue, e)
    
    otn = load_csv_graph(args.otn_topology_file)
    ip = load_csv_graph(args.ip_topology_file)
    ip_util_matrix = {}
    otn_util_matrix = {}
    for edge in ip.edges(data="all"):
        ip_util_matrix[(edge[0], edge[1], edge[2]['order'])] = 0.0

    for edge in otn.edges():
        otn_util_matrix[(edge[0], edge[1])] = 0.0
    total_vns = 0
    accepted_vns = 0
    rejected_vns = 0
    while not len(event_queue) <= 0:
        e = heapq.heappop(event_queue)
        print e.debug_string()
        vn = load_csv_graph(args.vnr_directory + "/" + e.vn_id)
        if e.etype == "departure":
            # if the embedding of vn_id was not successful at the first place do
            # nothing. This can be checked by reading from $(vn_id).status file.
            # If there was a successful embedding increase graph's capacity.
            status = get_embedding_status(args.vnr_directory + "/" + e.vn_id + ".status").strip("\r\n")
            print status
            if status == "Optimal" or status == "Success":
                active_vns.remove(e.vn_id)
                ip = update_ip_capacity(ip,vn,ip_util_matrix,args.vnr_directory + "/" + e.vn_id + ".emap",increase = True)
                write_csv_graph(ip, args.ip_topology_file)
                write_ip_util_matrix(ip, ip_util_matrix, "sim-data/util-data/ip_util." + str(e.ts))
        elif e.etype == "arrival":
            # run embedding first. if embedding is successful decrease the
            # capacity of SN. Otherwise do nothing.
            total_vns += 1
            otn_topology_file = get_full_path(current_directory, args.otn_topology_file)
            ip_topology_file = get_full_path(current_directory, args.ip_topology_file)
            ip_node_mapping_file = get_full_path(current_directory, args.ip_node_mapping_file)
            ip_link_mapping_file = get_full_path(current_directory, args.ip_link_mapping_file)
            ip_port_info_file = get_full_path(current_directory, args.ip_port_info_file)
            vn_topology_file = get_full_path(current_directory, args.vnr_directory + "/" + e.vn_id)
            vn_location_file = get_full_path(current_directory, 
                                                     args.vnr_directory + "/" +
                                                     e.vn_id + "loc")
            num_shuffles = 15
            for i in range(0, 5):
                execute_one_experiment(args.executable, otn_topology_file,
                        ip_topology_file, ip_node_mapping_file,
                        ip_link_mapping_file, ip_port_info_file, vn_topology_file,
                        vn_location_file, num_shuffles)               
                status = get_embedding_status(args.vnr_directory  + "/" + e.vn_id + ".status")             
                if status == "Optimal" or status == "Success":
                    ip, port_capacities, num_ports = update_ip_topology(ip, args.vnr_directory + "/" + e.vn_id + ".new_ip", ip_util_matrix, port_capacities, num_ports)
                    ip = update_ip_capacity(ip, vn, ip_util_matrix,
                            args.vnr_directory + "/" + e.vn_id + ".emap", 
                            increase = False)
                    otn = update_otn_capacity(otn, ip, otn_util_matrix,
                            args.vnr_directory + "/" + e.vn_id + ".new_ip")
                    write_csv_graph(ip, args.ip_topology_file)
                    write_csv_graph(otn, args.otn_topology_file)
                    write_ip_util_matrix(ip, ip_util_matrix, "sim-data/util-data/ip_util." + str(e.ts))
                    write_otn_util_matrix(otn, otn_util_matrix,"sim-data/util-data/otn_util." + str(e.ts))
                    accepted_vns += 1
                    active_vns.append(e.vn_id)
                    break
        with open("sim-data/sim-results", "a") as f:
            f.write(",".join([str(e.ts),str(total_vns), str(accepted_vns)]) + "\n")            
    print "total = " + str(total_vns) + ", accepted = " + str(accepted_vns)

if __name__ == "__main__":
    main()
    

