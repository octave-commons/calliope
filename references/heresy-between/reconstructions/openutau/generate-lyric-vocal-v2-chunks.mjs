import fs from 'node:fs';

const outBase = '/home/err/devel/Music/fork-tales/references/heresy-between/reconstructions/openutau';
const notesPath = `${outBase}/heresy-between-ritsu-lyric-vocal-v2.notes.json`;
const chunksDir = `${outBase}/v2-chunks`;
const manifestPath = `${outBase}/heresy-between-ritsu-lyric-vocal-v2-chunks.json`;
const ticksPerSecond = 800;
const chunks = [
  { id: 'chunk-01-intro-verse1', start: 0, end: 58 },
  { id: 'chunk-02-chorus1', start: 58, end: 106 },
  { id: 'chunk-03-verse2-pre2', start: 106, end: 174 },
  { id: 'chunk-04-chorus2-bridge', start: 174, end: 244 },
  { id: 'chunk-05-bridge-final-outro', start: 244, end: 303.32 },
];

if (fs.existsSync(manifestPath)) {
  throw new Error(`Refusing to overwrite existing artifact: ${manifestPath}`);
}
fs.mkdirSync(chunksDir, { recursive: true });

const source = JSON.parse(fs.readFileSync(notesPath, 'utf8'));

function noteYaml(notes) {
  return notes.map((note) => `  - position: ${note.position}
    duration: ${note.duration}
    tone: ${note.tone}
    lyric: ${note.lyric}
    pitch:
      data:
      - x: -40
        y: 0
        shape: io
      - x: 40
        y: 0
        shape: io
      snap_first: true
    vibrato:
      length: 0
      period: 175
      depth: 18
      in: 10
      out: 10
      shift: 0
      drift: 0
      vol_link: 0
    phoneme_expressions: []
    phoneme_overrides: []`).join('\n');
}

function ustxFor(chunk, notes) {
  const duration = Math.ceil((chunk.end - chunk.start) * ticksPerSecond);
  return `name: heresy-between-ritsu-lyric-vocal-v2-${chunk.id}
comment: 'Render-safe chunk for Heresy lyric vocal v2. Canonical full-song plan remains heresy-between-ritsu-lyric-vocal-v2.ustx.'
output_dir: Export
cache_dir: UCache
ustx_version: '0.6'
resolution: 480
key: 0
time_signatures:
- bar_position: 0
  beat_per_bar: 4
  beat_unit: 4
tempos:
- position: 0
  bpm: 100
tracks:
- singer: 波音リツ連続音Ver1.5.1
  phonemizer: OpenUtau.Plugin.Builtin.JapaneseVCVPhonemizer
  renderer_settings:
    renderer: WORLDLINE-R
  track_name: Ritsu lyric repair vocal ${chunk.id}
  track_color: Blue
  mute: false
  solo: false
  volume: 0
  pan: 0
  voice_color_names:
  - ''
voice_parts:
- duration: ${duration}
  name: Ritsu lyric vocal v2 ${chunk.id}
  track_no: 0
  position: 0
  notes:
${noteYaml(notes)}
`;
}

const chunkRecords = [];
for (const chunk of chunks) {
  const startTick = Math.round(chunk.start * ticksPerSecond);
  const selected = source.notes
    .filter((note) => {
      const start = note.position / ticksPerSecond;
      return start >= chunk.start && start < chunk.end;
    })
    .map((note) => ({
      position: note.position - startTick,
      duration: note.duration,
      tone: note.tone,
      lyric: note.lyric,
      section: note.section,
      source_text: note.source_text,
    }))
    .sort((a, b) => a.position - b.position);

  const path = `${chunksDir}/heresy-between-ritsu-lyric-vocal-v2-${chunk.id}.ustx`;
  if (fs.existsSync(path)) {
    throw new Error(`Refusing to overwrite existing artifact: ${path}`);
  }
  fs.writeFileSync(path, ustxFor(chunk, selected), 'utf8');
  chunkRecords.push({
    id: chunk.id,
    start_seconds: chunk.start,
    end_seconds: chunk.end,
    duration_seconds: Number((chunk.end - chunk.start).toFixed(2)),
    ustx_path: path,
    planned_wav_path: path.replace(/\.ustx$/, '.wav'),
    note_count: selected.length,
  });
}

const manifest = {
  generated_at: new Date().toISOString(),
  kind: 'OpenUTAU lyric vocal v2 render chunks',
  canonical_full_ustx: `${outBase}/heresy-between-ritsu-lyric-vocal-v2.ustx`,
  source_notes_json: notesPath,
  reason: 'Full-song v2 render loaded and phonemized but OpenUTAU exited 137, likely render-time memory pressure. These chunks are transport artifacts for lower-memory rendering.',
  chunks: chunkRecords,
};

fs.writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`, 'utf8');
console.log(JSON.stringify({ manifestPath, chunkCount: chunkRecords.length, chunks: chunkRecords }, null, 2));
