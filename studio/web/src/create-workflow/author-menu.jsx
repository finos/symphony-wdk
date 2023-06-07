import { atoms } from '../core/atoms';
import { Button, DropdownMenu, DropdownMenuItem } from "@symphony-ui/uitoolkit-components/components";
import { useEffect, useState, useRef } from 'react';
import { useRecoilState } from 'recoil';
import api from '../core/api';
import ReassignModal from './reassign-modal';
import SecretsModal from './secrets-modal';
import styled from 'styled-components';

const FloatingMenu = styled(DropdownMenu)`
    position: absolute;
    left: ${props => props.x}px;
    top: ${props => props.y}px;
    width: ${props => props.w}px;
    min-width: 11rem;
`;

const DropdownButton = styled(Button)`
    width: 100%;
    display: flex;
    justify-content: space-between;
    :after {
        font-family: tk-icons !important;
        content: '';
        font-size: 1rem;
        color: var(--tk-main-text-color, #525760);
    }
`;

const AuthorMenu = ({ uiService }) => {
    const session = useRecoilState(atoms.session)[0];
    const author = useRecoilState(atoms.author)[0];
    const { getUser } = api();
    const [ authorUser, setAuthorUser ] = useState();
    const [ showMenu, setShowMenu ] = useState(false);
    const [ showReassignModal, setShowReassignModal ] = useState(false);
    const [ showSecretsModal, setShowSecretsModal ] = useState(false);
    const buttonRef = useRef();

    useEffect(() => {
        if (!author) {
            return;
        }
        setAuthorUser(undefined);
        getUser(author, (user) => setAuthorUser(user));
    }, [ author ]);

    const AuthorButton = () => (
        <div ref={buttonRef}>
            <DropdownButton
                variant="secondary"
                onClick={() => setShowMenu((show) => !show)}
                loading={!authorUser}
            >
                @{authorUser?.displayName}
            </DropdownButton>
        </div>
    );

    const getBottomAnchor = () => {
        const rect = buttonRef.current?.getBoundingClientRect();
        return {
            w: rect?.width,
            x: rect?.left,
            y: !rect ? 0 : (rect.top + rect.height),
        };
    };

    const AuthorMenu = () => buttonRef.current && (
        <FloatingMenu show { ...getBottomAnchor() } onClick={() => setShowMenu(false)}>
            { author !== session.id && <DropdownMenuItem onClick={() => uiService.openIMbyUserIDs([ author ])}>Chat with Author</DropdownMenuItem> }
            { author === session.id && <DropdownMenuItem>You own this workflow</DropdownMenuItem> }
            <DropdownMenuItem onClick={() => setShowSecretsModal(true)}>Manage Secrets</DropdownMenuItem>
            { session.admin && <DropdownMenuItem onClick={() => setShowReassignModal(true)}>Reassign Owner</DropdownMenuItem> }
        </FloatingMenu>
    );

    return !authorUser ? <Button loading disabled /> : (
        <div style={{ flex: 1 }}>
            <AuthorButton />
            { showMenu && <AuthorMenu /> }
            { showReassignModal && <ReassignModal setShow={setShowReassignModal} /> }
            { showSecretsModal && <SecretsModal setShow={setShowSecretsModal} /> }
        </div>
    );
};
export default AuthorMenu;
