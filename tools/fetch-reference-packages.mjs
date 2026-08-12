import { writeFileSync, mkdirSync } from 'fs';

// ---------------------------------------------------------------------------
// Builds the reference list of popular npm packages used as the "known good"
// set for typosquat detection. Run once: node tools/fetch-reference-packages.mjs
// ---------------------------------------------------------------------------

const SEEDS = [
  'react','vue','angular','svelte','next','express','koa','fastify','nest','node',
  'test','jest','mocha','cypress','playwright','eslint','prettier','babel','webpack','vite',
  'typescript','rollup','esbuild','parcel','gulp','grunt','npm','yarn','pnpm','lerna',
  'http','axios','fetch','request','graphql','rest','api','socket','websocket','grpc',
  'aws','azure','google','cloud','docker','kubernetes','terraform','serverless','lambda','s3',
  'database','sql','postgres','mysql','mongo','redis','sqlite','orm','prisma','sequelize',
  'parser','logger','date','time','string','array','object','math','crypto','hash',
  'auth','jwt','oauth','password','security','validate','sanitize','escape','encode','decode',
  'stream','buffer','queue','cache','worker','thread','async','promise','event','emitter',
  'css','sass','less','tailwind','styled','ui','component','icon','animation','chart',
  'image','video','audio','pdf','excel','csv','json','yaml','xml','markdown',
  'file','path','fs','glob','watch','copy','zip','compress','upload','download',
  'cli','command','prompt','color','spinner','table','progress','shell','exec','env',
  'router','state','redux','store','form','input','modal','dropdown','calendar','map',
  'email','sms','notification','payment','stripe','analytics','tracking','monitor','metric','log'
];
// Packages that MUST be in the reference set regardless of what search returns.
// Topic-keyword search structurally misses brand-named packages (jquery, lodash,
// mongoose), which are exactly the highest-value squat targets. See docs/data-notes.md.
const MUST_INCLUDE = [
  'lodash','underscore','ramda','jquery','bluebird','async','request',
  'node-fetch','cross-env','dotenv','uuid','classnames','prop-types',
  'redux','react-redux','vue','vuex','svelte','jest','mocha',
  'chai','sinon','webpack','rollup','browserify','mongoose','sequelize',
  'knex','pg','mysql2','redis','passport','bcrypt','bcryptjs',
  'jsonwebtoken','nodemailer','sharp','jimp','puppeteer','cheerio',
  'yargs','inquirer','ora','boxen','figlet','nanoid','shortid',
  'immer','zustand','formik','yup','joi','zod','luxon','dayjs',
  'date-fns','numeral','papaparse','xlsx','archiver','handlebars',
  'ejs','pug','marked','highlight.js','moment-timezone','validator',
  'nodemon','concurrently','rimraf','mkdirp','fs-extra','execa'
];

const SEARCH_URL   = 'https://registry.npmjs.org/-/v1/search';
const DOWNLOAD_URL = 'https://api.npmjs.org/downloads/point/last-week';
const TARGET_SIZE  = 5000;
const OUTPUT_PATH  = 'src/main/resources/reference-packages.csv';

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/** Fetch with retry — the npm APIs occasionally return 429 (too many requests). */
async function fetchWithRetry(url, attempts = 3) {
  for (let i = 0; i < attempts; i++) {
    try {
      const res = await fetch(url);
      if (res.ok) return res;
      if (res.status === 429) {
        const wait = 2000 * (i + 1);
        console.warn(`  rate limited, waiting ${wait}ms`);
        await sleep(wait);
        continue;
      }
      console.warn(`  HTTP ${res.status}`);
      return null;
    } catch (err) {
      console.warn(`  network error: ${err.message}`);
      await sleep(1500);
    }
  }
  return null;
}

/** STAGE 1 — cast a wide net for candidate package names. */
async function collectNames() {
  const names = new Set();

  for (let s = 0; s < SEEDS.length; s++) {
    const seed = SEEDS[s];

    for (const from of [0, 250, 500]) {
      const url = `${SEARCH_URL}?text=${encodeURIComponent(seed)}`
                + `&size=250&from=${from}`
                + `&popularity=1.0&quality=0.0&maintenance=0.0`;

      const res = await fetchWithRetry(url);
      if (!res) continue;

      const data = await res.json();
      if (!data.objects || data.objects.length === 0) break;

      for (const entry of data.objects) {
        const name = entry.package.name;
        if (name.startsWith('@')) continue;   // bulk downloads API can't handle scoped names
        names.add(name);
      }

      await sleep(120);
    }

    console.log(`[${s + 1}/${SEEDS.length}] "${seed}" -> pool is ${names.size}`);
  }

  return [...names];
}
/**
 * STAGE 1b — mine dependency lists.
 *
 * Keyword search finds packages by how they describe themselves. This finds them
 * by who depends on them, which surfaces widely-used libraries whose descriptions
 * don't match any topic keyword.
 */
async function expandViaDependencies(names, sampleSize = 400) {
  const found = new Set();
  const sample = names.slice(0, sampleSize);

  for (let i = 0; i < sample.length; i++) {
    const url = `https://registry.npmjs.org/${sample[i]}/latest`;
    const res = await fetchWithRetry(url, 2);
    if (!res) continue;

    try {
      const meta = await res.json();
      for (const group of ['dependencies', 'peerDependencies']) {
        for (const dep of Object.keys(meta[group] || {})) {
          if (!dep.startsWith('@')) found.add(dep);
        }
      }
    } catch {
      // malformed metadata on a single package should not stop the run
    }

    if ((i + 1) % 50 === 0) {
      console.log(`  dependency scan ${i + 1}/${sample.length} -> ${found.size} names`);
    }
    await sleep(100);
  }

  return [...found];
}
/** STAGE 2 — get real weekly download counts, 128 names at a time. */
async function fetchDownloads(names) {
  const results = [];
  const batches = Math.ceil(names.length / 128);

  for (let i = 0; i < names.length; i += 128) {
    const batch = names.slice(i, i + 128);
    const url = `${DOWNLOAD_URL}/${batch.join(',')}`;

    const res = await fetchWithRetry(url);
    if (!res) { await sleep(1000); continue; }

    const data = await res.json();
    for (const [name, info] of Object.entries(data)) {
      if (info && typeof info.downloads === 'number' && info.downloads > 0) {
        results.push([name, info.downloads]);
      }
    }

    const batchNum = Math.floor(i / 128) + 1;
    console.log(`[batch ${batchNum}/${batches}] have ${results.length} with counts`);
    await sleep(150);
  }

  return results;
}

/** Escape a value for safe CSV output. */
function csvCell(value) {
  const s = String(value);
  return /[",\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}

// --------------------------------- main ------------------------------------

console.log('Stage 1: collecting candidate names...\n');
const discovered = await collectNames();
console.log(`\nStage 1 done: ${discovered.length} names from keyword search\n`);

console.log('Stage 1b: mining dependency lists...\n');
const fromDeps = await expandViaDependencies(discovered);
console.log(`\nStage 1b done: ${fromDeps.length} names seen in dependency lists\n`);

const candidates = [...new Set([...discovered, ...fromDeps, ...MUST_INCLUDE])];
console.log(`Candidate pool: ${candidates.length} unique names\n`);

console.log('Stage 2: fetching download counts...\n');
const withCounts = await fetchDownloads(candidates);
console.log(`\nStage 2 done: ${withCounts.length} packages with counts\n`);

withCounts.sort((a, b) => b[1] - a[1]);
const top = withCounts.slice(0, TARGET_SIZE);

mkdirSync('src/main/resources', { recursive: true });
writeFileSync(
  OUTPUT_PATH,
  'name,weekly_downloads\n' +
    top.map(([n, d]) => `${csvCell(n)},${d}`).join('\n') + '\n'
);

console.log(`Wrote ${top.length} packages to ${OUTPUT_PATH}`);
console.log(`Most downloaded:  ${top[0][0]} (${top[0][1].toLocaleString()}/week)`);
console.log(`Least downloaded: ${top[top.length - 1][0]} (${top[top.length - 1][1].toLocaleString()}/week)`);