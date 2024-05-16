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
    ],
    encoding: "utf-8"
  };

  // Apparently sbt can output ANSI escape codes. I've only seen one so far,
  // and it has only ever been printed when running sbt from this bit of Javascript
  // when running on any GitHub runner. I spent seven hours trying to debug this.
  // I decided to just look for the problematic character and filter it out.
  const result =
    process.platform === 'win32' ?
      spawnSync("sbt.bat", args.map(x => `"${x}"`), { shell: true, ...options }) :
      spawnSync("sbt", args, options);

  if (result.error)
    throw result.error;
  else if (result.status !== 0)
    throw new Error(`sbt process failed with exit code ${result.status}`);
  else
    return result.stdout.toString().replace('[0J', '').trim();
}

export default defineConfig(({ command, mode }) => {
  const linkOutputDir =
    command === "serve" ?
      printSbtTask("fastLinkOutputDir") :
      printSbtTask("fullLinkOutputDir");

  return {
    root: "src/main/web",
    publicDir: "dynamic",
    resolve: { alias: [ { find: "@linkOutputDir", replacement: linkOutputDir } ] },
    json: { stringify: true },
    worker: { format: "es" },
    server: { port: 5173, strictPort: true },
    preview: { port: 5173, strictPort: true },
    build: { outDir: "../../../target/vite", emptyOutDir: true }
  }
});
