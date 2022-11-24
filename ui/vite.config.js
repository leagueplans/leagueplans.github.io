import { spawnSync } from "child_process";
import { defineConfig } from "vite";

function printSbtTask(task) {
  const args = ["--error", "--batch", `print ${task}`];
  const options = {
    cwd: "../",
    stdio: [
      "pipe", // StdIn
      "pipe", // StdOut
      "inherit" // StdErr
    ]
  };

  const result =
    process.platform === 'win32' ?
      spawnSync("sbt.bat", args.map(x => `"${x}"`), { shell: true, ...options }) :
      spawnSync("sbt", args, options);

  if (result.error)
    throw result.error;
  else if (result.status !== 0)
    throw new Error(`sbt process failed with exit code ${result.status}`);
  else
    return result.stdout.toString('utf8').trim();
}

export default defineConfig(({ command, mode }) => {
  const linkOutputDir =
    command === "serve" ?
      printSbtTask("fastLinkOutputDir") :
      printSbtTask("fullLinkOutputDir");

  return {
    root: "src/main/web",
    publicDir: "dynamic",
    base: "/osrs-planner/",
    resolve: { alias: [ { find: "@linkOutputDir", replacement: linkOutputDir } ] },
    server: {
      port: 5173,
      strictPort: true
    },
    preview: {
      port: 5173,
      strictPort: true
    },
    build: { outDir: "../../../target/vite" }
  }
});
