import fs from 'node:fs';

const base = '/home/err/devel/Music/fork-tales/references/heresy-between';
const outBase = `${base}/reconstructions/openutau`;
const chunksDir = `${outBase}/v4-hybrid-safe-chunks`;
const gesturePath = `${base}/lyrics/auditions/gesture-layer-v3-hybrid-safe.json`;
const manifestPath = `${outBase}/heresy-between-hybrid-safe-openutau-v4.json`;
const ticksPerSecond = 800;
const targetDuration = 303.32;
const chunks = [
  { id: 'chunk-01', start: 0, end: 58 },
  { id: 'chunk-02', start: 58, end: 106 },
  { id: 'chunk-03', start: 106, end: 174 },
  { id: 'chunk-04', start: 174, end: 244 },
  { id: 'chunk-05', start: 244, end: 303.32 },
];

if (fs.existsSync(manifestPath)) {
  throw new Error(`Refusing to overwrite existing artifact: ${manifestPath}`);
}
fs.mkdirSync(chunksDir, { recursive: true });

const gesture = JSON.parse(fs.readFileSync(gesturePath, 'utf8'));

const kanaPool = [
  'あ','い','う','え','お','か','き','く','け','こ','さ','し','す','せ','そ','た','ち','つ','て','と',
  'な','に','ぬ','ね','の','は','ひ','ふ','へ','ほ','ま','み','む','め','も','や','ゆ','よ','ら','り','る','れ','ろ',
  'わ','ん'
];

function kanaForEvent(event, index) {
  const text = String(event.text || '');
  const kanaChars = [...text].filter((ch) => /[ぁ-んァ-ン]/u.test(ch) && ch !== 'っ' && ch !== 'ッ');
  if (kanaChars.length > 0) return kanaChars[index % kanaChars.length];
  return kanaPool[index % kanaPool.length];
}

function yamlNotes(notes) {
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
      depth: ${note.duration >= 480 ? 12 : 0}
      in: 15
      out: 15
      shift: 0
      drift: 0
      vol_link: 0
    phoneme_expressions: []
    phoneme_overrides: []`).join('\n');
}

function ustxForChunk(chunk, notes) {
  const duration = Math.ceil((chunk.end - chunk.start) * ticksPerSecond);
  return `name: heresy-between-hybrid-safe-openutau-v4-${chunk.id}
comment: 'Hybrid-safe OpenUTAU audition v4 from conservative transcript layer + isolated vocal f0. Unresolved/hallucinated regions are intentionally omitted.'
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
  track_name: Ritsu hybrid-safe vocal v4 ${chunk.id}
  track_color: Blue
  mute: false
  solo: false
  volume: 0
  pan: 0
  voice_color_names:
  - ''
voice_parts:
- duration: ${duration}
  name: Ritsu hybrid-safe vocal v4 ${chunk.id}
  track_no: 0
  position: 0
  notes:
${yamlNotes(notes)}
`;
}

const records = [];
for (const chunk of chunks) {
  const chunkStartTick = Math.round(chunk.start * ticksPerSecond);
  const events = gesture.events
    .filter((event) => event.start_s >= chunk.start && event.start_s < chunk.end)
    .map((event, index) => {
      const startTick = Math.max(0, Math.round(event.start_s * ticksPerSecond) - chunkStartTick);
      const durationTicks = Math.max(64, Math.round((event.end_s - event.start_s) * ticksPerSecond));
      return {
        position: startTick,
        duration: durationTicks,
        tone: Math.max(48, Math.min(72, Math.round(event.midi))),
        lyric: kanaForEvent(event, index),
        source_text: event.text,
        source_start_s: event.start_s,
        source_end_s: event.end_s,
        articulation: event.articulation,
      };
    })
    .sort((a, b) => a.position - b.position);

  const ustxPath = `${chunksDir}/heresy-between-hybrid-safe-openutau-v4-${chunk.id}.ustx`;
  if (fs.existsSync(ustxPath)) {
    throw new Error(`Refusing to overwrite existing artifact: ${ustxPath}`);
  }
  fs.writeFileSync(ustxPath, ustxForChunk(chunk, events), 'utf8');
  records.push({
    ...chunk,
    duration_seconds: Number((chunk.end - chunk.start).toFixed(3)),
    ustx_path: ustxPath,
    planned_wav_path: ustxPath.replace(/\.ustx$/, '.wav'),
    note_count: events.length,
    notes: events,
  });
}

const noteSeconds = records.flatMap((record) => record.notes).reduce((sum, note) => sum + note.duration / ticksPerSecond, 0);
const manifest = {
  generated_at: new Date().toISOString(),
  kind: 'Hybrid-safe gesture-derived OpenUTAU audition v4',
  source_gesture_layer: gesturePath,
  source_word_layer: gesture.word_layer,
  source_f0: gesture.f0_source,
  target_duration_seconds: targetDuration,
  singer: '波音リツ連続音Ver1.5.1',
  phonemizer: 'OpenUtau.Plugin.Builtin.JapaneseVCVPhonemizer',
  renderer: 'WORLDLINE-R',
  total_note_count: records.reduce((sum, record) => sum + record.note_count, 0),
  total_note_seconds: Number(noteSeconds.toFixed(3)),
  note_coverage_percent: Number((100 * noteSeconds / targetDuration).toFixed(3)),
  chunks: records.map(({notes, ...record}) => record),
  note_strategy: 'Use only conservative hybrid-safe transcript gesture events. Kana is sourced from accepted ASR segment text when available, otherwise safe kana pool. This branch intentionally leaves unresolved/hallucinated regions silent to test whether lyric trust improves over v3 dense-legato.',
};
fs.writeFileSync(manifestPath, JSON.stringify(manifest, null, 2) + '\n', 'utf8');
console.log(JSON.stringify({manifestPath, totalNoteCount: manifest.total_note_count, noteCoveragePercent: manifest.note_coverage_percent, chunks: manifest.chunks.map(c => ({id:c.id, noteCount:c.note_count, ustx:c.ustx_path}))}, null, 2));
