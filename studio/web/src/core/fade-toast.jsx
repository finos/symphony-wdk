import { atoms } from './atoms';
import { useRecoilState } from 'recoil';
import { Toast } from "@symphony-ui/uitoolkit-components/components";
import styled, { keyframes } from "styled-components";

const fade = keyframes`
    0%, 100% { opacity: 0 }
    10%, 90% { opacity: 1 }
`;

const Root = styled(Toast)`
    animation: ${fade} ${props => props.duration}s linear;
    z-index: 100;
    cursor: pointer;
`;

const FadeToast = () => {
    const [ status, setStatus ] = useRecoilState(atoms.status);
    const getBackgroundColor = () => status.error ? 'red' : 'green';

    return (
        <Root
            show={status.show}
            duration={status.error ? 10 : 3}
            content={status.content || ''}
            leftIcon={status.error ? 'cross' : 'check'}
            placement={{ horizontal: 'center', vertical: 'bottom' }}
            style={{ background: `var(--tk-color-${getBackgroundColor()}-50)` }}
            onClick={() => setStatus({ show: false })}
        />
    );
};
export default FadeToast;
