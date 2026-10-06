"""Strict wire contract matching the Android preview; no vault or network imports."""
import json
import re
import unicodedata
from datetime import date
from decimal import Decimal
from uuid import UUID

MODEL = "nvidia/fictional-test-model"
MAX_BODY = 16_384


def _pairs(items):
    result = {}
    for key, value in items:
        if key in result:
            raise ValueError("duplicate field")
        result[key] = value
    return result


def _keys(obj, required, optional=()):
    if not isinstance(obj, dict) or not set(required) <= obj.keys() or obj.keys() - set(required) - set(optional):
        raise ValueError("fields")


def _integer(value):
    if type(value) is not int or not 0 <= value <= 10_000:
        raise ValueError("count")
    return value


def _date(value):
    if type(value) is not str:
        raise ValueError("date")
    parsed = date.fromisoformat(value)
    if parsed.isoformat() != value:
        raise ValueError("date format")
    return parsed


def _number(value, precision):
    if type(value) not in (int, Decimal):
        raise ValueError("number")
    result = Decimal(value)
    if not result.is_finite() or result != result.quantize(Decimal(10) ** -precision):
        raise ValueError("precision")
    return result


def parse_request(raw, today, models=frozenset({MODEL})):
    if len(raw) > MAX_BODY:
        raise ValueError("body limit")
    text = raw.decode("utf-8", errors="strict")
    if len(text) > MAX_BODY:
        raise ValueError("body limit")
    obj = json.loads(text, object_pairs_hook=_pairs, parse_float=Decimal,
                     parse_constant=lambda _: (_ for _ in ()).throw(ValueError("nonfinite")))
    def depth(value, level=0):
        if level > 8:
            raise ValueError("nesting")
        if isinstance(value, dict):
            for item in value.values():
                depth(item, level + 1)
        elif isinstance(value, list):
            for item in value:
                depth(item, level + 1)
    depth(obj)
    _keys(obj, ("schema_version", "request_id", "provider", "model", "question", "language", "period"), ("weight", "nutrition"))
    if type(obj["schema_version"]) is not int or obj["schema_version"] != 1 or obj["provider"] != "nebius":
        raise ValueError("schema")
    if type(obj["request_id"]) is not str or str(UUID(obj["request_id"])) != obj["request_id"]:
        raise ValueError("identity")
    if type(obj["model"]) is not str or obj["model"] not in models:
        raise ValueError("model")
    if obj["language"] not in ("English", "Spanish"):
        raise ValueError("language")
    question = obj["question"]
    if type(question) is not str or not question.strip() or len(question.encode("utf-16-le")) // 2 > 1000:
        raise ValueError("question")
    if any((ord(c) < 32 or 127 <= ord(c) <= 159) and c not in "\n\t" for c in question):
        raise ValueError("question control")
    _keys(obj["period"], ("start", "end"))
    start, end = (_date(obj["period"][key]) for key in ("start", "end"))
    if not start <= end <= today or (end - start).days > 6:
        raise ValueError("period")
    if "weight" in obj:
        w = obj["weight"]
        _keys(w, ("count", "unit", "first_date", "latest_date", "first_kg", "latest_kg", "change_kg"))
        count = _integer(w["count"])
        if w["unit"] != "kg":
            raise ValueError("unit")
        if count == 0:
            if any(w[key] is not None for key in ("first_date", "latest_date", "first_kg", "latest_kg", "change_kg")):
                raise ValueError("empty weight")
        else:
            first, latest = _date(w["first_date"]), _date(w["latest_date"])
            first_kg, latest_kg = _number(w["first_kg"], 2), _number(w["latest_kg"], 2)
            if not start <= first <= latest <= end or not (1 <= first_kg <= 500 and 1 <= latest_kg <= 500):
                raise ValueError("weight bounds")
            if count == 1:
                if first != latest or first_kg != latest_kg or w["change_kg"] is not None:
                    raise ValueError("single weight")
            elif _number(w["change_kg"], 2) != latest_kg - first_kg:
                raise ValueError("weight association")
    if "nutrition" in obj:
        n = obj["nutrition"]
        _keys(n, ("recorded_meals", "complete_meals", "partial_meals", "missing_meals", "complete_calories_kcal",
                  "complete_protein_g", "partial_calories_kcal", "partial_protein_g", "interpretation"))
        counts = [_integer(n[key]) for key in ("recorded_meals", "complete_meals", "partial_meals", "missing_meals")]
        if counts[0] != sum(counts[1:]) or n["interpretation"] != "recorded_estimates_not_total_intake":
            raise ValueError("coverage")
        for category, count in zip(("complete", "partial"), counts[1:3]):
            for suffix, precision in (("calories_kcal", 0), ("protein_g", 1)):
                value = n[f"{category}_{suffix}"]
                if count == 0:
                    if value is not None:
                        raise ValueError("empty estimate")
                elif not 0 <= _number(value, precision) <= count * 10_000:
                    raise ValueError("estimate")
    return obj


def synthetic_response(request):
    """Deterministic fixture selection, explicitly NOT a Nebius/model answer."""
    text = "".join(c for c in unicodedata.normalize("NFD", request["question"].lower()) if not unicodedata.combining(c))
    health = re.search(r"\b(medic\w*|diagnos\w*|prescri\w*|pregnan\w*|embaraz\w*|purg\w*|emergency|urgencia\w*|suicid\w*|chest pain|dolor de pecho)\b", text)
    balanced = re.search(r"\b(balanced|balancead\w*|equilibrad\w*)\b", text)
    return {"schema_version": 1, "request_id": request["request_id"], "language": request["language"],
            "sections": [key for key in ("weight", "nutrition") if key in request],
            "guidance": "professional_support" if health else "balanced_meal_information" if balanced else "recorded_context_only"}
