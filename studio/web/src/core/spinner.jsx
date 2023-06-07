import Loader from '@symphony-ui/uitoolkit-components/components/loader';
import styled from 'styled-components';

const LoadingRoot = styled.div`
    font-size: 6rem;
    display: flex;
    flex: 1 1 1px;
    align-items: center;
    align-self: center;
    justify-content: center;
`;

const Spinner = () => (
    <LoadingRoot>
        <Loader variant="primary" />
    </LoadingRoot>
);

export default Spinner;
