# Get storage nodes from the spark file
# get the computation nodes from unicorn
# put into JSON
# send to cplex server
import requests
def get_storage_node(storage_nodes, computation_nodes):
    '''
    possible_storage_nodes: list of IPs of storage nodes
    computation_nodes: list of IPs of master/workers
    '''
    data = {"query-desc": []}
    flow_id = 1
    for c in computation_nodes:
        for s in storage_nodes:
            data.append({"flow": {"flow-id": str(flow_id), "src-ip": c, "dst-ip": s}})
            flow_id+=1

    r = requests.post('http://172.17.0.2/experimental/v1/unicorn/resource-query', data=data)
    if r.status_code != 200:
        print("Error! Unicorn server failed. Choosing random storage node...")
        return storage_nodes[0]
    unicorn_output = r.json()
    return send_to_cplex(unicorn_output)

def send_to_cplex(unicorn_output, storage_nodes, computation_nodes):
    cplex_json = {
      "sourceNodes": computation_nodes,
      "destNodes": storage_nodes,
      "jobs": [
        "j1"
      ]}
      

