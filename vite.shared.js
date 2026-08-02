import { spawnSync } from "child_process";
import { dirname, resolve } from "path";
import { fileURLToPath } from "url";

const repoRoot = dirname(fileURLToPath(import.meta.url));
const uicommonWeb = resolve(repoRoot, "uicommon/src/main/web");

/** Aliases pointing at assets owned by uicommon.
 *
 * Its Scala sources import stylesheets by absolute path, and those paths would otherwise
 * be resolved against whichever project's web root Vite happens to be serving. Both
 * projects that build on uicommon need these.
 */
export function uicommonAliases() {
  return [
    { find: "@uicommon", replacement: uicommonWeb },
    { find: "/styles/common", replacement: resolve(uicommonWeb, "styles/common") },
    { find: "/styles/floating", replacement: resolve(uicommonWeb, "styles/floating") }
  ];
}

/** Runs the Scala.js linker for one project and returns where it put its output. */
export function linkOutputDir(project, command) {
  const task = command === "serve" ? "fastLinkOutputDir" : "fullLinkOutputDir";
  return printSbtTask(`${project}/${task}`);
}

function printSbtTask(task) {
  const args = ["--error", "--batch", `print ${task}`];
  const options = {
    cwd: repoRoot,
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
    return result.stdout.toString().replace('[0J', '').trim();
}
