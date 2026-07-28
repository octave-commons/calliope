import fs from 'node:fs';

const base = '/home/err/devel/Music/fork-tales/references/heresy-between';
const outBase = `${base}/reconstructions/openutau`;
const f0Path = `${base}/vocal/vocals.pyin.f0.json`;
const ustxPath = `${outBase}/heresy-between-ritsu-lyric-vocal-v2.ustx`;
const notesPath = `${outBase}/heresy-between-ritsu-lyric-vocal-v2.notes.json`;
const manifestPath = `${outBase}/heresy-between-openutau-vocal-repair-v2.json`;

for (const path of [ustxPath, notesPath, manifestPath]) {
  if (fs.existsSync(path)) {
    throw new Error(`Refusing to overwrite existing artifact: ${path}`);
  }
}

const ticksPerSecond = 800;
const trackDurationSeconds = 303.32;
const voiceDuration = Math.ceil(trackDurationSeconds * ticksPerSecond);
const f0 = JSON.parse(fs.readFileSync(f0Path, 'utf8')).frames;

const sections = [
  {
    section: 'Intro breath/spoken',
    start: 0,
    end: 12,
    lines: [
      { text: '（息）…はぁ…', kana: 'はあ' },
      { text: '「元気？」', kana: 'げんき' },
      { text: '三文字だけで、世界が揺れる', kana: 'さんもじだけでせかいがゆれる' },
    ],
  },
  {
    section: 'Verse 1A',
    start: 12,
    end: 24,
    lines: [
      { text: '「元気だよ」って', kana: 'げんきだよて' },
      { text: '一行だけのメッセージ', kana: 'いちぎょうだけのめせえじ' },
      { text: '既読の灯りが', kana: 'きどくのあかりが' },
      { text: '胸の底を叩いてる', kana: 'むねのそこをたたいてる' },
    ],
  },
  {
    section: 'Verse 1B',
    start: 24,
    end: 40,
    lines: [
      { text: '言えなかった「ごめんね」より', kana: 'いえなかたごめんねより' },
      { text: 'もっと怖いのは', kana: 'もとこわいのは' },
      { text: '“何も送らない”っていう', kana: 'なにもおくらないていう' },
      { text: '静かな選択', kana: 'しずかなせんたく' },
    ],
  },
  {
    section: 'Pre-Chorus 1',
    start: 40,
    end: 58,
    lines: [
      { text: '理由はいつも', kana: 'りゆうはいつも' },
      { text: 'あとから並べられる', kana: 'あとからならべられる' },
      { text: 'でも指先は先に', kana: 'でもゆびさきはさきに' },
      { text: '生き方を押してしまう', kana: 'いきかたをおしてしまう' },
    ],
  },
  {
    section: 'Chorus 1A',
    start: 58,
    end: 82,
    lines: [
      { text: '一行だけのメッセージで', kana: 'いちぎょうだけのめせえじで' },
      { text: 'この夜の形が決まるなら', kana: 'このよるのかたちがきまるなら' },
      { text: '僕は逃げ道の言い訳より', kana: 'ぼくはにげみちのいいわけより' },
      { text: '小さな責任を選びたい（ねえ）', kana: 'ちいさなせきにんをえらびたいねえ' },
    ],
  },
  {
    section: 'Chorus 1B',
    start: 82,
    end: 106,
    lines: [
      { text: 'さよならの続き書けなくて', kana: 'さよならのつづきかけなくて' },
      { text: '空白ばかり増えていくけど', kana: 'くうはくばかりふえていくけど' },
      { text: '一行だけのメッセージで', kana: 'いちぎょうだけのめせえじで' },
      { text: '“意味”を置いて帰るよ', kana: 'いみをおいてかえるよ' },
    ],
  },
  {
    section: 'Verse 2A',
    start: 106,
    end: 128,
    lines: [
      { text: 'タイムラインには', kana: 'たいむらいんには' },
      { text: '知らない誰かの笑顔', kana: 'しらないだれかのえがお' },
      { text: '強がってスクロールして', kana: 'つよがてすくろおるして' },
      { text: '心だけ取り残す', kana: 'こころだけとりのこす' },
    ],
  },
  {
    section: 'Verse 2B',
    start: 128,
    end: 150,
    lines: [
      { text: '打ちかけの言葉が', kana: 'うちかけのことばが' },
      { text: '震えて消えてくたび', kana: 'ふるえてきえてくたび' },
      { text: '「どうせ…」の物語が', kana: 'どうせのものがたりが' },
      { text: 'また王冠をかぶる', kana: 'またおうかんをかぶる' },
    ],
  },
  {
    section: 'Pre-Chorus 2',
    start: 150,
    end: 174,
    lines: [
      { text: '“きっと決まってる”って', kana: 'きときまてるて' },
      { text: '口にした瞬間', kana: 'くちにしたしゅんかん' },
      { text: '僕の手のひらから', kana: 'ぼくのてのひらから' },
      { text: '明日が落ちていく', kana: 'あしたがおちていく' },
    ],
  },
  {
    section: 'Chorus 2A',
    start: 174,
    end: 198,
    lines: [
      { text: '一行だけのメッセージで', kana: 'いちぎょうだけのめせえじで' },
      { text: '戻れる場所を探す僕', kana: 'もどれるばしょをさがすぼく' },
      { text: '正しさは外に落ちてなくて', kana: 'ただしさはそとにおちてなくて' },
      { text: '選んだ跡に立ち上がる（ああ）', kana: 'えらんだあとにたちあがるああ' },
    ],
  },
  {
    section: 'Chorus 2B',
    start: 198,
    end: 222,
    lines: [
      { text: '言えなかったこと数えたら', kana: 'いえなかたことかぞえたら' },
      { text: '画面に入りきらないほど', kana: 'がめんにはいりきらないほど' },
      { text: 'それでも一行だけで', kana: 'それでもいちぎょうだけで' },
      { text: '世界に参加していたい', kana: 'せかいにさんかしていたい' },
    ],
  },
  {
    section: 'Bridge',
    start: 222,
    end: 244,
    lines: [
      { text: 'もしも明日', kana: 'もしもあした' },
      { text: 'ただの友だちで笑えたら', kana: 'ただのともだちでわらえたら' },
      { text: 'それだけでいい', kana: 'それだけでいい' },
      { text: 'そう言い聞かせて', kana: 'そういいきかせて' },
    ],
  },
  {
    section: 'Bridge reveal',
    start: 244,
    end: 262,
    lines: [
      { text: 'でも本当は知ってる', kana: 'でもほんとうはしってる' },
      { text: '沈黙もまた', kana: 'ちんもくもまた' },
      { text: '誰かの夜を', kana: 'だれかのよるを' },
      { text: '増やしてしまうって', kana: 'ふやしてしまうて' },
    ],
  },
  {
    section: 'Breath/leap',
    start: 262,
    end: 274,
    lines: [
      { text: '（息）…はぁ…', kana: 'はあ' },
      { text: '打ち直す「元気？」の三文字', kana: 'うちなおすげんきのさんもじ' },
      { text: '跳ぶのは信仰じゃなくて', kana: 'とぶのはしんこうじゃなくて' },
      { text: '“投げ出さない”という決意', kana: 'なげださないというけつい' },
    ],
  },
  {
    section: 'Final Chorus',
    start: 274,
    end: 296,
    lines: [
      { text: '一行だけのメッセージで', kana: 'いちぎょうだけのめせえじで' },
      { text: 'つながる夜があるのなら', kana: 'つながるよるがあるのなら' },
      { text: '返事なんてもう来なくても', kana: 'へんじなんてもうこなくても' },
      { text: '名前を呼べた気がするよ', kana: 'なまえをよべたきがするよ' },
      { text: '“どうせ”の続き書けなくて', kana: 'どうせのつづきかけなくて' },
      { text: 'それでも送信を押してみる', kana: 'それでもそうしんをおしてみる' },
      { text: '一行だけのメッセージで', kana: 'いちぎょうだけのめせえじで' },
      { text: '僕は明日を肯定する', kana: 'ぼくはあしたをこうていする' },
    ],
  },
  {
    section: 'Outro',
    start: 296,
    end: 303.32,
    lines: [
      { text: '（息）…', kana: 'はあ' },
      { text: '一行だけ', kana: 'いちぎょうだけ' },
      { text: 'それで十分だ', kana: 'それでじゅうぶんだ' },
    ],
  },
];

const scale = [60, 61, 63, 65, 66, 68, 70, 72];
const contours = {
  calm: [0, 0, 1, 0, -1, 0, 1, 0],
  rise: [-1, 0, 1, 2, 1, 0, 1, 2],
  chorus: [0, 1, 2, 3, 2, 1, 2, 0],
  fall: [2, 1, 0, -1, 0, -1, -2, -1],
};

function chars(kana) {
  return [...kana].filter((ch) => /[ぁ-んァ-ン]/u.test(ch) && ch !== 'っ' && ch !== 'ッ');
}

function median(values) {
  if (values.length === 0) return null;
  const sorted = [...values].sort((a, b) => a - b);
  return sorted[Math.floor(sorted.length / 2)];
}

function nearestScaleTone(midi) {
  let tone = Math.round(midi);
  while (tone > 72) tone -= 12;
  while (tone < 60) tone += 12;
  return scale.reduce((best, candidate) => Math.abs(candidate - tone) < Math.abs(best - tone) ? candidate : best, scale[0]);
}

function medianTone(start, end, fallback) {
  const mids = f0
    .filter((frame) => frame.time >= start && frame.time <= end && frame.voiced && frame.midi !== null && frame.probability >= 0.05)
    .map((frame) => frame.midi);
  const med = median(mids);
  return med === null ? fallback : nearestScaleTone(med);
}

function sectionContour(section) {
  if (section.includes('Chorus') || section.includes('Final')) return contours.chorus;
  if (section.includes('Pre') || section.includes('leap')) return contours.rise;
  if (section.includes('Outro') || section.includes('Bridge')) return contours.fall;
  return contours.calm;
}

function clampTone(tone) {
  return Math.max(60, Math.min(72, tone));
}

function yamlScalar(value) {
  return String(value).replaceAll('\\', '\\\\').replaceAll("'", "''");
}

const notes = [];
const phraseRecords = [];

for (const sec of sections) {
  const sectionSpan = sec.end - sec.start;
  const lineSpan = sectionSpan / sec.lines.length;
  const contour = sectionContour(sec.section);
  const baseFallback = sec.section.includes('Chorus') || sec.section.includes('Final') ? 65 : sec.section.includes('Bridge') ? 61 : 63;

  sec.lines.forEach((line, lineIndex) => {
    const lineStart = sec.start + lineIndex * lineSpan + Math.min(0.18, lineSpan * 0.06);
    const lineEnd = sec.start + (lineIndex + 1) * lineSpan - Math.min(0.22, lineSpan * 0.08);
    const syllables = chars(line.kana);
    if (syllables.length === 0 || lineEnd <= lineStart) return;

    const lineDuration = lineEnd - lineStart;
    const gap = Math.min(0.035, lineDuration / Math.max(1, syllables.length) * 0.18);
    const noteDuration = Math.max(0.08, (lineDuration - gap * (syllables.length - 1)) / syllables.length);
    const baseTone = medianTone(lineStart, lineEnd, baseFallback);
    const phraseNotes = [];

    syllables.forEach((lyric, index) => {
      const start = lineStart + index * (noteDuration + gap);
      const isTail = index === syllables.length - 1;
      const duration = isTail ? Math.min(noteDuration * 1.35, lineEnd - start) : noteDuration;
      const position = Math.round(start * ticksPerSecond);
      const ticks = Math.max(64, Math.round(duration * ticksPerSecond));
      const tone = clampTone(baseTone + contour[index % contour.length]);
      const note = { position, duration: ticks, tone, lyric, sourceText: line.text, section: sec.section };
      notes.push(note);
      phraseNotes.push(note);
    });

    phraseRecords.push({
      section: sec.section,
      text: line.text,
      kana: line.kana,
      start_seconds: Number(lineStart.toFixed(3)),
      end_seconds: Number(lineEnd.toFixed(3)),
      note_count: phraseNotes.length,
      median_tone: baseTone,
    });
  });
}

notes.sort((a, b) => a.position - b.position);

let totalNoteSeconds = 0;
let lastEnd = 0;
const gaps = [];
for (const note of notes) {
  const start = note.position / ticksPerSecond;
  const end = (note.position + note.duration) / ticksPerSecond;
  totalNoteSeconds += Math.max(0, end - start);
  if (start - lastEnd > 0.001) gaps.push({ start, end: start, duration: start - lastEnd });
  lastEnd = Math.max(lastEnd, end);
}
if (trackDurationSeconds - lastEnd > 0.001) {
  gaps.push({ start: lastEnd, end: trackDurationSeconds, duration: trackDurationSeconds - lastEnd });
}

const longGaps = gaps.filter((gap) => gap.duration > 4);
const coveragePercent = (totalNoteSeconds / trackDurationSeconds) * 100;

const noteYaml = notes.map((note) => `  - position: ${note.position}
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

const ustx = `name: heresy-between-ritsu-lyric-vocal-v2
comment: 'Lyric-aware OpenUTAU vocal repair v2 for Heresy. Section-anchored kana-safe Ritsu VCV performance; generated from repair plan.'
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
  track_name: Ritsu lyric repair vocal
  track_color: Blue
  mute: false
  solo: false
  volume: 0
  pan: 0
  voice_color_names:
  - ''
voice_parts:
- duration: ${voiceDuration}
  name: Ritsu lyric-aware vocal repair v2
  track_no: 0
  position: 0
  notes:
${noteYaml}
`;

const notesManifest = {
  generated_at: new Date().toISOString(),
  kind: 'OpenUTAU lyric-aware vocal repair notes v2',
  bpm: 100,
  ticks_per_second: ticksPerSecond,
  track_duration_seconds: trackDurationSeconds,
  voice_duration: voiceDuration,
  ustx_path: ustxPath,
  note_count: notes.length,
  total_note_seconds: Number(totalNoteSeconds.toFixed(2)),
  coverage_percent: Number(coveragePercent.toFixed(2)),
  gap_count: gaps.length,
  max_gap_seconds: Number(Math.max(...gaps.map((gap) => gap.duration)).toFixed(2)),
  gaps_over_4s: longGaps.map((gap) => ({
    start_seconds: Number(gap.start.toFixed(2)),
    end_seconds: Number(gap.end.toFixed(2)),
    duration_seconds: Number(gap.duration.toFixed(2)),
  })),
  phrase_count: phraseRecords.length,
  phrases: phraseRecords,
  notes: notes.map((note) => ({
    position: note.position,
    duration: note.duration,
    tone: note.tone,
    lyric: note.lyric,
    section: note.section,
    source_text: note.sourceText,
  })),
};

const manifest = {
  generated_at: notesManifest.generated_at,
  kind: 'OpenUTAU vocal repair v2',
  ustx_path: ustxPath,
  notes_json_path: notesPath,
  planned_vocal_wav_path: `${outBase}/heresy-between-ritsu-lyric-vocal-v2.wav`,
  source_repair_plan: `${outBase}/heresy-between-vocal-repair-plan-v2.md`,
  source_lyrics: '/home/err/devel/Music/heresy_between/存在論的な“反・虚無”の論証として/存在論的な“反・虚無”の論証として.txt',
  reference_vocal_stem: `${base}/stems/htdemucs/存在論的な“反・虚無”の論証として/vocals.wav`,
  f0_source: f0Path,
  singer: '波音リツ連続音Ver1.5.1',
  phonemizer: 'OpenUtau.Plugin.Builtin.JapaneseVCVPhonemizer',
  renderer: 'WORLDLINE-R',
  note_count: notesManifest.note_count,
  total_note_seconds: notesManifest.total_note_seconds,
  coverage_percent: notesManifest.coverage_percent,
  max_gap_seconds: notesManifest.max_gap_seconds,
  gaps_over_4s: notesManifest.gaps_over_4s,
  acceptance: {
    minimum_coverage_percent: 60,
    preferred_coverage_percent: 70,
    max_unintentional_gap_seconds: 4,
    coverage_pass: notesManifest.coverage_percent >= 60,
    gap_pass: notesManifest.gaps_over_4s.length === 0,
  },
};

fs.writeFileSync(ustxPath, ustx, 'utf8');
fs.writeFileSync(notesPath, `${JSON.stringify(notesManifest, null, 2)}\n`, 'utf8');
fs.writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`, 'utf8');

console.log(JSON.stringify({
  ustxPath,
  notesPath,
  manifestPath,
  noteCount: notes.length,
  coveragePercent: notesManifest.coverage_percent,
  maxGapSeconds: notesManifest.max_gap_seconds,
  gapsOver4s: notesManifest.gaps_over_4s.length,
}, null, 2));
