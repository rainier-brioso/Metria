"""Code-owned, fictional scenarios for exercising the bounded live provider."""
import json
from datetime import date, datetime, timedelta, timezone
import re
from uuid import uuid4

from contract import parse_request

SCENARIOS = ("weight", "nutrition", "combined", "empty", "general")

_WEIGHT_QUESTION = "What does my recorded weight show this week?"
_NUTRITION_QUESTION = "What nutrition estimates have I recorded this week?"
_COMBINED_QUESTION = "Summarize my recorded weight and nutrition this week."
_EMPTY_QUESTION = "What does my recorded weight show this week?"
_GENERAL_QUESTION = "What makes a balanced meal?"


def _weight(start, end):
    first = min(start + timedelta(days=1), end)
    return {"count": 2, "unit": "kg", "first_date": first.isoformat(), "first_kg": 70.0,
            "latest_date": end.isoformat(), "latest_kg": 69.5, "change_kg": -0.5}


def _nutrition():
    return {"recorded_meals": 3, "complete_meals": 1, "partial_meals": 1, "missing_meals": 1,
            "complete_calories_kcal": 451, "complete_protein_g": 21.2,
            "partial_calories_kcal": 120, "partial_protein_g": 4.6,
            "interpretation": "recorded_estimates_not_total_intake"}


def build_sample(scenario, model, request_id=None, today=None):
    """Return a strict parsed request and its wire bytes, containing no caller data."""
    if scenario not in SCENARIOS:
        raise ValueError("unknown demo scenario")
    if type(model) is not str or not re.fullmatch(r"nvidia/[A-Za-z0-9][A-Za-z0-9._-]{0,119}", model):
        raise ValueError("NVIDIA model required")
    day = today or datetime.now(timezone.utc).date()
    if type(day) is not date:
        raise ValueError("date required")
    start = day - timedelta(days=6)
    request = {"schema_version": 1, "request_id": request_id or str(uuid4()), "provider": "nebius",
               "model": model, "language": "English", "period": {"start": start.isoformat(), "end": day.isoformat()}}
    if scenario == "weight":
        request.update(question=_WEIGHT_QUESTION, weight=_weight(start, day))
    elif scenario == "nutrition":
        request.update(question=_NUTRITION_QUESTION, nutrition=_nutrition())
    elif scenario == "combined":
        request.update(question=_COMBINED_QUESTION, weight=_weight(start, day), nutrition=_nutrition())
    elif scenario == "empty":
        request.update(question=_EMPTY_QUESTION,
                       weight={"count": 0, "unit": "kg", "first_date": None, "first_kg": None,
                               "latest_date": None, "latest_kg": None, "change_kg": None})
    else:
        request.update(question=_GENERAL_QUESTION)
    preview = json.dumps(request, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    parsed = parse_request(preview, day, frozenset({model}))
    return parsed, preview


def expected_selection(request, scenario):
    """Build the exact expected safe selection, to check more than schema validity."""
    if scenario not in SCENARIOS:
        raise ValueError("unknown demo scenario")
    sections = [name for name in ("weight", "nutrition") if name in request]
    if scenario == "weight" and sections != ["weight"]:
        raise ValueError("scenario/request mismatch")
    if scenario == "nutrition" and sections != ["nutrition"]:
        raise ValueError("scenario/request mismatch")
    if scenario == "combined" and sections != ["weight", "nutrition"]:
        raise ValueError("scenario/request mismatch")
    if scenario == "empty" and (sections != ["weight"] or request["weight"]["count"] != 0):
        raise ValueError("scenario/request mismatch")
    if scenario == "general" and sections:
        raise ValueError("scenario/request mismatch")
    return {"schema_version": 1, "request_id": request["request_id"], "language": "English",
            "sections": sections,
            "guidance": "balanced_meal_information" if scenario == "general" else "recorded_context_only"}
