#!/usr/bin/env python3
"""Validate LearnThai story content before it ships.

Hard errors (exit 1):
  * missing/invalid fields, duplicate ids
  * token segmentation that does not concatenate back to the Thai sentence
  * references to missing characters, images, audio, or story files
  * unknown words that are neither in the 300-word curriculum nor declared in extra_words
  * a token `match` that is not in the curriculum

Advisory (warnings):
  * new-word budget, line length limits, phases used above the story's declared phases

Usage:
    python tools/validate_stories.py
"""

import io
import json
import os
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets")
STORIES = os.path.join(ASSETS, "stories")
CURRICULUM = os.path.join(ASSETS, "thai_curriculum_100.json")

REQUIRED_STORY_FIELDS = ["id", "title_en", "teaches_label", "characters", "cards"]
REQUIRED_CARD_FIELDS = ["id", "speaker", "image", "thai", "english", "tokens"]

errors = []
warnings = []


def error(message):
    errors.append(message)


def warn(message):
    warnings.append(message)


def load_json(path):
    with io.open(path, encoding="utf-8") as fh:
        return json.load(fh)


def load_curriculum():
    if not os.path.exists(CURRICULUM):
        warn("curriculum file missing; skipping word coverage checks")
        return {}
    return {w["thai_word"]: w for w in load_json(CURRICULUM)}


def check_card_ids(story, story_id):
    seen = set()
    for card in story["cards"]:
        if card["id"] in seen:
            error(f"{story_id}: duplicate card id {card['id']}")
        seen.add(card["id"])


def check_tokens(story, story_id):
    for card in story["cards"]:
        joined = "".join(t["thai"] for t in card["tokens"])
        if joined != card["thai"]:
            error(
                f"{story_id}/{card['id']}: tokens do not rebuild the sentence\n"
                f"    tokens: {joined}\n    thai:   {card['thai']}"
            )
        if not card["tokens"]:
            error(f"{story_id}/{card['id']}: no tokens")

        for token in card["tokens"]:
            if not token.get("gloss"):
                warn(f"{story_id}/{card['id']}: token '{token['thai']}' has no glossary entry")


def check_assets(story, story_id):
    for card in story["cards"]:
        image = card.get("image", "")
        if not image:
            error(f"{story_id}/{card['id']}: no image")
        elif not os.path.exists(os.path.join(STORIES, image)):
            error(f"{story_id}/{card['id']}: missing art {image}")

        audio = card.get("audio")
        if audio and not os.path.exists(os.path.join(STORIES, audio)):
            error(f"{story_id}/{card['id']}: missing audio {audio}")


def check_characters(story, story_id):
    ids = {c["id"] for c in story["characters"]}
    if not ids:
        error(f"{story_id}: no characters")
    for card in story["cards"]:
        if card["speaker"] not in ids:
            error(f"{story_id}/{card['id']}: unknown speaker '{card['speaker']}'")
        listener = card.get("listener")
        if listener and listener not in ids:
            error(f"{story_id}/{card['id']}: unknown listener '{listener}'")


def check_vocabulary(story, story_id, curriculum):
    declared = {w["thai"] for w in story.get("extra_words", [])}
    unknown = {}

    for card in story["cards"]:
        for token in card["tokens"]:
            # Punctuation is presentation: "เปิด!" is still the word เปิด.
            thai = token["thai"].strip("!?.,…")
            if thai in curriculum:
                continue
            if thai in declared:
                continue
            unknown.setdefault(thai, card["id"])

            match = token.get("match")
            if match and match not in curriculum:
                error(f"{story_id}/{card['id']}: token '{thai}' matches unknown word '{match}'")

    for word, card_id in unknown.items():
        error(f"{story_id}/{card_id}: '{word}' is not in the curriculum and not in extra_words")

    extras = len(story.get("extra_words", []))
    if extras > 24:
        warn(f"{story_id}: {extras} extra words (a scene is usually better under ~20)")

    if curriculum:
        total = sum(len(c["tokens"]) for c in story["cards"])
        known = sum(
            1
            for c in story["cards"]
            for t in c["tokens"]
            if t["thai"].strip("!?.,…") in curriculum
        )
        pct = (known / total * 100) if total else 0
        print(f"  coverage: {known}/{total} tokens ({pct:.0f}%) already in the word deck")

    for card in story["cards"]:
        if len(card.get("new_words", [])) > 3:
            warn(f"{story_id}/{card['id']}: {len(card['new_words'])} new words in one line")


def check_lengths(story, story_id):
    for card in story["cards"]:
        if len(card["thai"]) > 60:
            warn(f"{story_id}/{card['id']}: Thai line is {len(card['thai'])} chars (may overflow the bubble)")
        if len(card["english"]) > 90:
            warn(f"{story_id}/{card['id']}: English line is {len(card['english'])} chars (may need scrolling)")


def check_phases(story, story_id, curriculum):
    phases = story.get("phases") or []
    if not phases:
        warn(f"{story_id}: no phases declared")
        return
    limit = max(phases)
    for card in story["cards"]:
        for token in card["tokens"]:
            word = curriculum.get(token["thai"])
            if word and word.get("phase", 0) > limit:
                warn(
                    f"{story_id}/{card['id']}: '{token['thai']}' is phase {word['phase']} "
                    f"(above declared max {limit})"
                )


def main():
    if not os.path.exists(os.path.join(STORIES, "index.json")):
        error("assets/stories/index.json is missing")
        report()
        return

    curriculum = load_curriculum()
    index = load_json(os.path.join(STORIES, "index.json"))
    stories = index.get("stories", [])

    if not stories:
        error("index.json lists no stories")

    seen_ids = set()
    for entry in stories:
        story_id = entry.get("id", "")
        print(f"checking {story_id}")

        if not story_id:
            error("index entry without id")
            continue
        if story_id in seen_ids:
            error(f"duplicate story id {story_id}")
        seen_ids.add(story_id)

        story_path = os.path.join(STORIES, entry.get("file", ""))
        if not os.path.exists(story_path):
            error(f"{story_id}: story file '{entry.get('file')}' is missing")
            continue

        cover = entry.get("cover", "")
        if not cover:
            error(f"{story_id}: no cover")
        elif not os.path.exists(os.path.join(STORIES, cover)):
            error(f"{story_id}: missing cover {cover}")

        story = load_json(story_path)

        for field in REQUIRED_STORY_FIELDS:
            if not story.get(field):
                error(f"{story_id}: missing '{field}'")

        if story.get("id") != story_id:
            error(f"{story_id}: story id '{story.get('id')}' does not match the index")

        for card in story.get("cards", []):
            for field in REQUIRED_CARD_FIELDS:
                if not card.get(field):
                    error(f"{story_id}/{card.get('id', '?')}: missing '{field}'")

        if story.get("cards"):
            check_card_ids(story, story_id)
            check_tokens(story, story_id)
            check_assets(story, story_id)
            check_characters(story, story_id)
            check_vocabulary(story, story_id, curriculum)
            check_lengths(story, story_id)
            check_phases(story, story_id, curriculum)

            if entry.get("line_count") not in (0, None, len(story["cards"])):
                warn(
                    f"{story_id}: index line_count {entry.get('line_count')} "
                    f"!= {len(story['cards'])} cards"
                )

    report()


def report():
    for w in warnings:
        print(f"  WARN  {w}")
    for e in errors:
        print(f"  ERROR {e}")

    print()
    print(f"{len(errors)} error(s), {len(warnings)} warning(s)")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
