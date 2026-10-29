# Editing the Metria demo

Checked 2026-10-28. Recommendation for this deadline: CapCut Desktop, using
recorded app footage, the prepared SRT, and English narration generated in blocks
or recorded with your own voice. No account connection, software installation,
paid subscription or upload has been performed by this preparation.

| Option | Why choose it | Practical limitation |
| --- | --- | --- |
| CapCut Desktop | Imports our SRT and supports built-in text-to-speech; convenient for cuts, labels and narration in one timeline | Voice/features and limits vary by account, region and version. Test a short sample before committing to a voice; do not assume all options are free |
| Clipchamp | Straightforward Windows workflow with text-to-speech and automatic captions | Do not plan on importing our SRT into the editor. Generate captions from the finished narration, then correct model names; player caption-upload features are a different workflow |
| Shotcut | Free, open-source editor with SRT import/export and a documented subtitle-to-speech workflow | More manual setup; check the installed version's speech engine/voice availability before relying on narration generation |

Official references:

- CapCut [subtitle import](https://www.capcut.com/help/how-to-import-subtitles),
  [desktop voice generation and availability](https://www.capcut.com/tools/ai-voice-generator-pc).
- Microsoft [Clipchamp text-to-speech](https://support.microsoft.com/en-us/clipchamp/how-to-use-the-text-to-speech-feature),
  [automatic captions](https://support.microsoft.com/en-us/clipchamp/how-to-use-autocaptions-in-clipchamp).
  The [Microsoft Q&A answer on subtitle import](https://learn.microsoft.com/en-us/answers/questions/5600127/how-do-i-upload-a-transcribed-file-to-clipchamp)
  describes the editor's limitation; verify the current installed UI before assuming import.
- Shotcut [subtitle, import/export and speech documentation](https://forum.shotcut.org/t/view-subtitles/45312).

## Suggested assembly order

1. Record short clips on the phone: photo/confirmation, returned draft/review,
   Save, question/answer, provider choice. Use only fictional content. Record
   local/Gemma behavior separately from cloud/Nemotron behavior.
2. Record the clean Docker startup on the PC, with the key hidden and the pass
   and credentials excluded from the frame. Keep technical footage brief.
3. Create a 1920 x 1080 landscape project, with the portrait phone footage large
   enough to read. Use spare side space for short model-role labels. Keep a
   bottom band for captions without covering the app's primary controls.
4. Generate/record narration from voiceover-en.txt in the script's scene blocks.
   Start with a short sample to check “Metria”, “Gemma”, “MiniLM”, “Nemotron” and
   “Nebius”. Use pronunciation aliases in the voice tool only, preserving correct
   written model names in subtitles. A voice sample must be listened to before
   generating the entire track; no audio is included in this repository yet.
5. Cut the app footage to the narration, keeping the actual draft and answer
   readable. Shorten waits only with a visible “Waiting time shortened” label.
6. Import captions-en.srt in a supporting editor and align its provisional times
   to the actual voice. In Clipchamp, generate automatic captions instead.
7. Export an MP4 at 1080p, review it end-to-end, and keep duration under three
   minutes. The script targets 2:28; actual spoken timing may differ.
8. Upload publicly to YouTube when the final file is checked; verify the link
   from a signed-out browser. Publishing is a separate explicit user action.

Use voice plus actual interaction as the main presentation. No avatar or stock
footage is needed for this demo. A lightweight architecture overlay is enough
to explain the model roles. The opening should reach the real photo result and
record-grounded calorie answer within 30 seconds.
