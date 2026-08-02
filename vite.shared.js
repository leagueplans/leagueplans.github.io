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

  const result =
    process.platform === 'win32' ?
      spawnSync("sbt.bat", args.map(x => `"${x}"`), { shell: true, ...options }) :
      spawnSync("sbt", args, options);

  if (result.error)
    throw result.error;
  else if (result.status !== 0)
    throw new Error(`sbt process failed with exit code ${result.status}`);
  else
    return stripEscapeSequences(result.stdout.toString()).trim();
}

/** Removes the ANSI escape sequences sbt mixes into its output.
 *
 * sbt only does this when it is invoked from here, and only on a GitHub runner. They have
 * to go, because the value becomes a Vite alias, and an alias with one embedded in it
 * resolves to a filename that cannot be opened.
 *
 * An escape sequence is a terminal instruction - "erase the screen", "go bold" - sent as
 * ordinary bytes in amongst the text. They all have the same shape: the ESC character
 * (0x1b), a `[`, digits saying how much to do, then a final letter saying what to do. The
 * pattern below spells that out one byte range at a time: ESC, `[`, parameter bytes
 * (0x30-0x3f, the digits and `;`), intermediate bytes (0x20-0x2f), then the final byte
 * (0x40-0x7e). sbt sends `ESC[0J` - "erase to the end of the screen".
 */
function stripEscapeSequences(output) {
  return output.replace(/\u001b\[[0-?]*[ -\/]*[@-~]/g, "");
}