import styled from 'styled-components';
import { Badge } from '@symphony-ui/uitoolkit-components';

const Root = styled.div`
    display: flex;
    flex: 1 1 1px;
    min-height: calc(100vh - 16rem);
`;

const VersionsPane = styled.div`
    flex: 1 1 1px;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: .5rem;
`;

const Version = styled.div`
    display: flex;
    flex-direction: column;
    border: var(--tk-color-graphite-40) 1px solid;
    gap: .3em;
    border-radius: .3rem;
    padding: .5rem;
    margin-right: .5rem;

    background-color: ${props => props.selected ? 'var(--tk-color-electricity-50)' : 'transparent'};
    color: ${props => props.selected ? '#fff' : 'var(--tk-main-text-color, #525760)'};

    :hover {
        cursor: pointer;
        color: #fff;
        background-color: var(--tk-color-electricity-40);
    }

    &:first-child:not(:only-child) {
        margin-bottom: 1rem;
        &:after {
            margin: -.2rem auto;
            position: relative;
            top: 1.1rem;
            content: ' ';
            width: 80%;
            height: 1px;
            background: var(--tk-color-graphite-30);
        }
    }
`;

const EditorPane = styled.div`
    display: flex;
    flex-direction: column;
    flex: 3 1 1px;
`;

const Editor = styled.div`
    flex: 1 1 1px;
`;

const ContainedBadge = styled(Badge)`
    overflow: hidden;
    text-overflow: ellipsis;
`;

const Labels = styled.div`
    display: flex;
    justify-content: space-around;
    margin-bottom: .5rem;
    padding-right: 2rem;
`;

export {
    Root, VersionsPane, Version, EditorPane, Editor, ContainedBadge, Labels
}
