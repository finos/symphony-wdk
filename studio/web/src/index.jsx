import { atoms } from './core/atoms';
import { RecoilRoot, useRecoilState } from 'recoil';
import api from './core/api';
import Console from './console/console';
import FadeToast from './core/fade-toast';
import React, { Suspense, useState, useEffect, lazy } from 'react';
import ReactDOM from 'react-dom/client';
import Spinner from './core/spinner';
import './index.css';
import styled from 'styled-components';

const AppRoot = styled.div`
    display: flex;
    flex-direction: column;
    flex: 1 1 1px;
    gap: .5rem;
    font-family: SymphonyLato, serif;
    font-size: 1rem;
    padding: .5rem;
    width: calc(100vw - 1rem);
`;

const Editor = lazy(() => import('./editor/editor'));
const WorkflowSelector = lazy(() => import('./create-workflow/workflow-selector'));
const ActionBar = lazy(() => import('./action-bar/action-bar'));
const Monitor = lazy(() => import('./monitor/monitor'));

const App = () => {
    const [ thisEditor, setThisEditor ] = useState();
    const [ showConsole, setShowConsole ] = useState(false);
    const editMode = useRecoilState(atoms.editMode)[0];
    const setTheme = useRecoilState(atoms.theme)[1];
    const [ session, setSession ] = useRecoilState(atoms.session);
    const [ uiService, setUiService ] = useState();
    const { parseJwt, getProfile } = api();

    const initSymphony = (appId) => window.SYMPHONY.remote.hello().then((data) => {
        const bodyClasses = []
        if (data.themeV2.name === 'dark') {
            bodyClasses.push('tk-dark');
        }
        if (data.themeV2.isCondensedMode) {
            bodyClasses.push('tk-condensed');
        }
        document.querySelector('body').className = bodyClasses.join(' ');
        setTheme(data.themeV2.name);

        SYMPHONY.application.connect(
            appId,
            [ 'modules', 'applications-nav', 'extended-user-info', 'ui' ],
            [ `${appId}:app` ],
        ).then(() => {
            const existingSession = JSON.parse(window.localStorage.getItem('session'));
            if (existingSession && new Date().getTime() < (existingSession.exp * 1000)) {
                setSession(existingSession);
            }
            const userInfoService = SYMPHONY.services.subscribe('extended-user-info');
            if (userInfoService) {
                userInfoService.getJwt().then((token) => {
                    if (token) {
                        getProfile(token, (response) => {
                            const jwt = parseJwt(token);
                            const newSession = { token, ...jwt, ...response };
                            setSession(newSession);
                            window.localStorage.setItem('session', JSON.stringify(newSession));
                        });
                    }
                });
            }
            setUiService(SYMPHONY.services.subscribe('ui'));
        }, (e) => console.error(e));
    });

    useEffect(() => {
        if (window.SYMPHONY) {
            const origin = window.origin === 'http://localhost:5173' ?
                'https://localhost:10443' : window.origin;
            fetch(`${origin}/bdk/v1/app/info`)
                .then(r => r.json())
                .then(({ appId }) => initSymphony(appId));
        }
    }, []);

    if (!window.SYMPHONY) {
        return 'Please launch Symphony to use WDK Studio';
    }
    return !session ? 'Loading..' : (
        <Suspense fallback={<Spinner />}>
            <AppRoot>
                <WorkflowSelector {...{ uiService }} />
                <ActionBar {...{ showConsole, setShowConsole, thisEditor }} />
                <Editor show={editMode} {...{ thisEditor, setThisEditor }} />
                { !editMode && <Monitor /> }
                <Console show={editMode && showConsole} />
                <FadeToast />
            </AppRoot>
        </Suspense>
    );
};
ReactDOM.createRoot(document.querySelector('#root')).render(<RecoilRoot><App /></RecoilRoot>);
