import styled from 'styled-components';

const InstanceMetricPanel = styled.div`
    padding: 1rem 3rem;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 2rem;
`;

const InstanceMetricItem = styled.div`
    border: 1px var(--tk-color-electricity-20) solid;
    border-radius: .3rem;
    padding: .5rem;
    text-align: center;

    &:last-child {
        border: 1px var(--tk-color-error, #ee3d3d) solid;
        color: var(--tk-color-error, #ee3d3d);
    }
`;

const InstanceMetricItemTitle = styled.div`
    font-weight: 600;
    margin-bottom: .8rem;
`;

const InstanceMetricItemNumber = styled.div`
    font-weight: 300;
    font-size: 2rem;
`;

const InstanceMetrics = ({ instances }) => {
    const statuses = [ 'Pending', 'Completed', 'Failed' ];
    const countInstances = (status) => instances.filter((i) => i.status === status.toUpperCase()).length;
    return (
        <InstanceMetricPanel>
            <InstanceMetricItem>
                <InstanceMetricItemTitle>Total</InstanceMetricItemTitle>
                <InstanceMetricItemNumber>{instances.length}</InstanceMetricItemNumber>
            </InstanceMetricItem>
            { statuses.map((status) => (
                <InstanceMetricItem key={status}>
                    <InstanceMetricItemTitle>{status}</InstanceMetricItemTitle>
                    <InstanceMetricItemNumber>{countInstances(status)}</InstanceMetricItemNumber>
                </InstanceMetricItem>
            ))}
        </InstanceMetricPanel>
    );
};
export default InstanceMetrics;
