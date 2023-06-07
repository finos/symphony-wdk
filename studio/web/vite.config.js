import { defineConfig } from 'vite';
import fs from 'fs';
import path from 'path';
import react from '@vitejs/plugin-react';

export default defineConfig({
    plugins: [ react(), reactVirtualized() ]
});

// Fix for bad import in react-virtualized
const WRONG_CODE = `import { bpfrpt_proptype_WindowScroller } from "../WindowScroller.js";`;
export function reactVirtualized() {
    return {
        name: "flat:react-virtualized",
        configResolved() {
            const file = require
                .resolve("react-virtualized")
                .replace(
                    path.join("dist", "commonjs", "index.js"),
                    path.join("dist", "es", "WindowScroller", "utils", "onScroll.js"),
                );
            const code = fs.readFileSync(file, "utf-8");
            const modified = code.replace(WRONG_CODE, "");
            fs.writeFileSync(file, modified);
        },
    };
};
