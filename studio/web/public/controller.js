const appUri = window.location.hostname === 'localhost' ?
    'http://localhost:5173' : window.location.origin;
let appId;
let appName;
let controllerId;
let controller;

const auth = () => fetch('/bdk/v1/app/auth', { method: 'POST' });
const getAppId = () => fetch('/bdk/v1/app/info');

const register = ({ appToken }) => SYMPHONY.application.register(
    { appId, tokenA: appToken },
    [ 'modules', 'applications-nav', 'extended-user-info' ],
    [ controllerId ]
);

const bootstrap = () => {
    let modulesService = SYMPHONY.services.subscribe("modules");
    let navService = SYMPHONY.services.subscribe("applications-nav");
    const meta = { title: appName, icon: appUri + '/icon-16.png' };
    navService.add(appId, meta, controllerId);
    controller.implement({
        select: (id) => {
            if (id === appId) {
                modulesService.show(`${appId}-app`, meta, controllerId, appUri);
            }
        },
    });
};

getAppId().then(r => r.json()).then((appInfo) => {
    appId = appInfo.appId;
    appName = `WDK Studio: ${appInfo.name}`;
    controllerId = `${appId}:controller`;
    controller = SYMPHONY.services.register(controllerId);
    SYMPHONY.remote.hello().then(auth).then(register).then(bootstrap);
});
