import styled, { keyframes } from "styled-components";

const DetailPlane = styled.div`
    display: flex;
    flex-direction: column;
    overflow-y: scroll;
    flex: 1 1 1px;
`;

const rotateAnimation = keyframes`
    0% { transform: rotate(0deg) }
    100% { transform: rotate(-360deg) }
`;

const TableTitle = styled.h3`
    font-weight: 800;
    color: var(--tk-color-graphite-10);
    background: var(--tk-color-electricity-70);
    padding: .4rem 1rem;
    margin: 0;
    display: flex;
    justify-content: space-between;
    & > div:last-child { color: var(--tk-color-graphite-10); cursor: pointer }
    .loading { animation: ${rotateAnimation} 1.3s linear infinite; }
`;

const Table = styled.table`
    font-weight: 300;
    border-collapse: collapse;
    width: fit-content;

    th {
        position: sticky;
        top: 0;
        z-index: 2;
        text-align: left;
        background-color: var(--tk-background, #fff);
    }
    th, td {
        white-space: nowrap;
        padding: .3rem .8rem;
        overflow: hidden;
        text-overflow: ellipsis;
    }
    tbody tr:hover {
        cursor: pointer;
        background: var(--tk-color-electricity-50);
        & td { color: var(--tk-color-electricity-05) }
        & button.inspect {
            color: #fff;
            border-color: #fff;
        }
    }
`;

const Row = styled.tr`
    background-color: ${props => props.selected ? 'var(--tk-color-electricity-40)' : ''};
    td {
        color: ${props => props.status === 'PENDING' ? 'var(--tk-color-green-30)' : ''};
        color: ${props => props.status === 'FAILED' ? 'var(--tk-color-error, #ee3d3d)' : ''};
        color: ${props => props.selected ? 'var(--tk-color-electricity-05)' : ''};

        &.indicator:after {
            color: ${props => props.selected ? 'var(--tk-color-electricity-05)' : 'transparent'};
            font-family: 'tk-icons';
            content: "";
        }

        & button.inspect {
            &:hover { background-color: var(--tk-color-electricity-30) }
            color: ${props => props.selected ? '#fff' : 'var(--tk-main-text-color, #525760)'};
            border-width: 1px;
            border-style: solid;
            border-color: ${props => props.selected ? '#fff' : 'var(--tk-main-text-color, #525760)'};
            box-shadow: none;
        }
    }
`;

export {
    DetailPlane, Table, TableTitle, Row
};
