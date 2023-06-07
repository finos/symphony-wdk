import styled from 'styled-components';
import { useState } from 'react';

const ResizeContainer = styled.div`
    position: fixed;
    left: 0;
    bottom: ${props => props.height + 50 - 40}px;
    width: calc(100% - 1rem);
    height: 80px;
    z-index: 99;
    user-select: none;
    :hover { cursor: ns-resize }
`;

const ResizeBar = ({ consoleHeight, setConsoleHeight }) => {
    const [ resize, setResize ] = useState({ drag: false });

    const handleResizeStart = ({ nativeEvent }) =>
        setResize({ drag: true, x: nativeEvent.offsetX, y: nativeEvent.offsetY });

    const handleResizeMove = ({ nativeEvent }) => {
        if (resize.drag) {
            setConsoleHeight(old => old + resize.y - nativeEvent.offsetY);
        }
    };

    const handleResizeEnd = () => setResize({ drag: false });

    return (
        <ResizeContainer
            height={consoleHeight}
            onMouseDown={handleResizeStart}
            onMouseMove={handleResizeMove}
            onMouseUp={handleResizeEnd}
            onMouseLeave={handleResizeEnd}
        />
    );
};
export default ResizeBar;
