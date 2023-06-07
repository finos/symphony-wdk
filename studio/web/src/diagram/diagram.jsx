import React, { useEffect, useState } from 'react';
import ReactFlow, {
    ConnectionLineType, useNodesState, useEdgesState, Background, MarkerType, applyEdgeChanges
} from 'reactflow';
import dagre from 'dagre';
import styled from 'styled-components';
import api from '../core/api';
import { atoms } from '../core/atoms';
import { useRecoilState } from 'recoil';
import './style.css';

const Root = styled.div`
    height: calc(100vh - 14rem);
`;

var dagreGraph = new dagre.graphlib.Graph();
dagreGraph.setDefaultEdgeLabel(() => ({}));

const nodeWidth = 172;
const nodeHeight = 60;
const position = { x: 0, y: 0 };
const edgeType = 'step';

const getLayoutedElements = (nodes, edges, direction = 'TB') => {
    const isHorizontal = direction === 'LR';
    dagreGraph.setGraph({ rankdir: direction });
    nodes.forEach((node) => dagreGraph.setNode(node.id, {width: nodeWidth, height: nodeHeight }));
    edges.forEach((edge) => dagreGraph.setEdge(edge.source, edge.target));
    dagre.layout(dagreGraph);

    nodes.forEach((node) => {
        const nodeWithPosition = dagreGraph.node(node.id);
        node.targetPosition = isHorizontal ? 'left' : 'top';
        node.sourcePosition = isHorizontal ? 'right' : 'bottom';

        node.position = {
            x: nodeWithPosition.x - nodeWidth / 2,
            y: nodeWithPosition.y - nodeHeight / 2,
        };
        return node;
    });
    return { nodes, edges };
};

const { nodes: layoutedNodes, edges: layoutedEdges } = getLayoutedElements([], []);

const Diagram = () => {
    const currentWorkflow = useRecoilState(atoms.currentWorkflow)[0];
    const selectedInstance = useRecoilState(atoms.selectedInstance)[0];
    const [ nodes, setNodes, onNodesChange ] = useNodesState(layoutedNodes);
    const [ edges, setEdges, onEdgesChange ] = useEdgesState(layoutedEdges);
    const [ activities, setActivities ] = useState();
    const { listWorkflowInstanceActivities, getWorkflowDefinition } = api();

    const reactFlowStyle = { width: '100%', height: '100%' };

    useEffect(() => {
        setNodes([]);
        setEdges([]);
        loadDefinition();
    }, []);

    useEffect(() => {
        if (selectedInstance) {
            listWorkflowInstanceActivities(selectedInstance.id, selectedInstance.instanceId, (r) => setActivities(r));
        }
    }, [ selectedInstance ]);

    useEffect(() => {
        if (!activities) {
            return;
        }
        const dateParams = { hour: 'numeric', minute: 'numeric', second: 'numeric' };
        const dateFormat = (d) => Intl.DateTimeFormat('en-US', dateParams).format(new Date(d));
        const greenProps = {
            animated: true,
            style: { stroke: '#65c862' },
            markerEnd: { type: MarkerType.ArrowClosed, color: '#65c862' },
        };
        const runPath = Object.fromEntries(activities.nodes.map((n) =>[ n.nodeId, dateFormat(n.endDate) ]));
        const edgeChanges = edges.map((edge) => {
            if (runPath[edge.source] && runPath[edge.target]) {
                return { item: { ...edge, ...greenProps, label: runPath[edge.source], }, type: 'reset' };
            }
            return { item: { ...edge, style: { opacity: .3 } }, type: 'reset' };
        });
        setEdges((existing) => applyEdgeChanges(edgeChanges, existing));
    }, [ activities ]);

    const loadDefinition = () => getWorkflowDefinition(currentWorkflow.value, (data) => {
        const nodes = new Array();
        const edges = new Array();
        const regexType = /(.*)(_)/gm;

        data?.flowNodes?.map((element) => {
            const isEvent = element.group === "EVENT";
            const isGateway = element.group === "GATEWAY";

            nodes.push({
                id: element.nodeId,
                data: {
                    label: (
                        <>
                            {isGateway
                                ? <div className={'react-flow__node-title-gateway'}>
                                    <img src={'./gateway_icon.png'} style={{width: '18px'}} />
                                </div>
                                : <span>
                                    <div className={'react-flow__node-title'} style={{background: isEvent ? 'var(--tk-color-green-50)' : 'var(--tk-color-electricity-50)'}}>
                                        <img src={(element.group=='EVENT') ? './event_icon.png' : './activity_icon.png'} style={{float: 'left', marginLeft: '4px', marginTop: '-4px', width: '12px'}} />
                                        <div style={{margin: '0 auto'}}>{element.type}</div>
                                    </div>
                                </span>
                            }
                            {!isGateway &&
                                <div className={'react-flow__node-content'}>
                                    {element.nodeId.replace(regexType, '')}
                                </div>
                            }
                        </>
                    )
                },
                position,
                type: (element.parents?.length==0) ? 'input' : (element.children?.length==0) ? 'output' : 'default',
                nodeType: element.group,
                style: {
                    border: isGateway ? '0px' : 'inherit',
                    boxShadow: isGateway ? 'inherit' : '0px'
                }
            });
            element.parents.map((edgeItem) => {
                edges.push({ id: Math.floor(Math.random() * 10000), source: edgeItem, target: element.nodeId, type: edgeType, animated: false, markerEnd: {type: MarkerType.ArrowClosed }})
            });
        });
        setNodes(nodes);
        setEdges(edges);
        getLayoutedElements(nodes, edges);
    });

    return (
        <Root>
            <ReactFlow
                style={reactFlowStyle}
                nodes={nodes}
                edges={edges}
                onNodesChange={onNodesChange}
                onEdgesChange={onEdgesChange}
                connectionLineType={ConnectionLineType.SmoothStep}
                elementsSelectable={false}
                nodesConnectable={false}
                nodesDraggable={true}
                panOnDrag={true}
                minZoom={0.3}
                fitView
                proOptions={{ hideAttribution: true }}
            >
                <Background variant="dots" gap={50} size={1} />
            </ReactFlow>
        </Root>
    );
};

export default Diagram;
