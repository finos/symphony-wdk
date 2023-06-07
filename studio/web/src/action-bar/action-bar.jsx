import { atoms } from '../core/atoms';
import { Button } from '@symphony-ui/uitoolkit-components/components';
import { useRecoilState } from 'recoil';
import styled from 'styled-components';
import DeleteButton from './delete-button';
import DiagramModal from './diagram-modal';
import SaveButton from './save-button';
import VersionsModal from './versions-modal';
import { DropdownMenu, DropdownMenuItem, Icon } from '@symphony-ui/uitoolkit-components/components';
import { useState } from 'react';
import WizardModal from './wizard-modal';

const Root = styled.div`
    display: flex;
    gap: .5rem;
    justify-content: space-between;
`;

const OverflowButton = styled(Button)`
    @media (min-width: 800px) {
        display: none;
    }
`;

const FloatingMenu = styled(DropdownMenu)`
    position: absolute;
    right: .5rem;
    top: 6.25rem;
    width: 8rem;
`;

const Section = styled.div`
    display: flex;
    gap: .5rem;
    @media (max-width: 800px) {
        &:not(:first-child) { display: none }
        & > button { display: none }
    }
`;

const ActionButton = (props) => (
    <Button variant="secondary" {...props}>
        {props.label}
    </Button>
);

const ActionBar = ({ showConsole, setShowConsole, thisEditor }) => {
    const session = useRecoilState(atoms.session)[0];
    const author = useRecoilState(atoms.author)[0];
    const markers = useRecoilState(atoms.markers)[0];
    const activeVersion = useRecoilState(atoms.activeVersion)[0];
    const currentWorkflow = useRecoilState(atoms.currentWorkflow)[0];
    const setSelectedInstance = useRecoilState(atoms.selectedInstance)[1];
    const [ editMode, setEditMode ] = useRecoilState(atoms.editMode);
    const [ showMenu, setShowMenu ] = useState(false);
    const [ showWizard, setShowWizard ] = useState(false);
    const [ showDiagram, setShowDiagram ] = useState(false);
    const [ showVersions, setShowVersions ] = useState(false);

    const toggleEditMode = () => {
        setEditMode(!editMode);
        setSelectedInstance(null);
    };

    const openHelp = () => window.open('//github.com/finos/symphony-wdk/blob/master/docs/reference.md', '_blank', false);

    return (
        <>
            <Root>
                <Section>
                    <SaveButton thisEditor={thisEditor} />
                    <ActionButton
                        label="Wizard"
                        disabled={!currentWorkflow || !editMode || author !== session.id}
                        onClick={() => setShowWizard(true)}
                    />
                    <ActionButton
                        label="Versions"
                        disabled={!activeVersion}
                        onClick={() => setShowVersions(true)}
                    />
                    <DeleteButton />
                </Section>
                <Section>
                    <ActionButton
                        label={ editMode ? 'Monitor' : 'Edit' }
                        disabled={!currentWorkflow || markers.length > 0}
                        onClick={toggleEditMode}
                    />
                    <ActionButton
                        label="Diagram"
                        disabled={!currentWorkflow || markers.length > 0}
                        onClick={() => setShowDiagram(true)}
                    />
                    <ActionButton
                        label={`${showConsole ? 'Hide' : 'Show'} Console`}
                        onClick={() => setShowConsole((old) => !old)}
                        disabled={!editMode}
                    />
                    <ActionButton label="Help" onClick={() => openHelp()} />
                </Section>
                <OverflowButton variant="secondary" onClick={() => setShowMenu((show) => !show)}>
                    Menu
                    <Icon iconName='drop-down' />
                </OverflowButton>
            </Root>

            { showWizard &&<WizardModal setShow={setShowWizard} /> }
            { showVersions && <VersionsModal setShow={setShowVersions} readOnly={author !== session.id} /> }
            { showDiagram && <DiagramModal setShow={setShowDiagram} /> }
            { showMenu && (
                <FloatingMenu show onClick={() => setShowMenu(false)}>
                    { currentWorkflow && editMode && <DropdownMenuItem onClick={() => setShowWizard(true)}>Wizard</DropdownMenuItem> }
                    { activeVersion && <DropdownMenuItem onClick={() => setShowVersions(true)}>Versions</DropdownMenuItem> }
                    <DropdownMenuItem onClick={toggleEditMode}>{ editMode ? 'Monitor' : 'Edit' }</DropdownMenuItem>
                    { currentWorkflow && markers.length === 0 && <DropdownMenuItem onClick={() => setShowDiagram(true)}>Diagram</DropdownMenuItem> }
                    { editMode && (
                        <DropdownMenuItem onClick={() => setShowConsole((old) => !old)}>
                            {showConsole ? 'Hide' : 'Show'} Console
                        </DropdownMenuItem>
                    )}
                    <DropdownMenuItem onClick={() => openHelp()}>Help</DropdownMenuItem>
                </FloatingMenu>
            )}
        </>
    );
};
export default ActionBar;
