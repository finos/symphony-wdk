import { DetailPlane, TableTitle, Table, Row } from './styles';

const InstanceList = ({ instances, selectedInstance, setSelectedInstance, loadInstances, loading }) => {
    const formatDuration = (duration) => duration?.toString()
        .substring(2)
        .replaceAll(/([\d\.]+)(\w)/g, "$1$2 ")
        .replaceAll(/([\d]+)\.(\d)([\d]+)/g, "$1.$2")
        .toLowerCase();

    return (
        <>
            <TableTitle>
                Instances
                <div onClick={loadInstances} className={loading ? 'loading' : ''}>&#8634;</div>
            </TableTitle>
            <DetailPlane>
                <Table>
                    <thead>
                        <Row>
                            <th></th>
                            <th>Start</th>
                            <th>End</th>
                            <th>Duration</th>
                            <th>Status</th>
                        </Row>
                    </thead>
                    <tbody>
                        {instances.map((row, i) => (
                            <Row
                                key={i}
                                status={row.status}
                                selected={row.instanceId === selectedInstance?.instanceId}
                                onClick={() => setSelectedInstance(row)}
                            >
                                <td className="indicator"></td>
                                <td>{(new Date(row.startDate)).toLocaleString()}</td>
                                <td>{row.endDate? (new Date(row.endDate)).toLocaleString() : 'Running...'}</td>
                                <td>{formatDuration(row.duration)}</td>
                                <td>{row.status}</td>
                            </Row>
                        ))}
                    </tbody>
                </Table>
            </DetailPlane>
        </>
    );
};
export default InstanceList;
