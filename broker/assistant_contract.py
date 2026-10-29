"""Provider-independent Metria policy/tool source, also compiled into Android."""
import hashlib
import json
import math
from pathlib import Path
import unicodedata

SOURCE = Path(__file__).resolve().parent.parent / "shared" / "assistant-contract.json"
RAW = SOURCE.read_bytes()
CONTRACT = json.loads(RAW)
if CONTRACT.get("version") != 1:
    raise ValueError("unsupported assistant contract")
SOURCE_SHA256 = hashlib.sha256(RAW).hexdigest()
BASE_PROMPT = CONTRACT["base_prompt"]
AGENT_POLICY = CONTRACT["agent_policy"]
EXAMPLES = CONTRACT["examples"]
TOOLS = {tool["name"]: tool for tool in CONTRACT["tools"]}
if len(TOOLS) != len(CONTRACT["tools"]) or not TOOLS:
    raise ValueError("invalid assistant tool catalog")
TASK_TOOLS = {task: frozenset(names) for task, names in CONTRACT["task_tools"].items()}
if any(not names <= TOOLS.keys() for names in TASK_TOOLS.values()):
    raise ValueError("unknown task tool")
CHAT_TOOLS = TASK_TOOLS["chat"]


def validate_arguments(name, arguments):
    tool = TOOLS.get(name)
    if tool is None or type(arguments) is not dict:
        return False
    parameters = {p["name"]: p for p in tool["parameters"]}
    if not arguments.keys() <= parameters.keys():
        return False
    for key, parameter in parameters.items():
        if key not in arguments:
            if parameter["required"]:
                return False
            continue
        value = arguments[key]
        if value is None:
            if not parameter["nullable"]:
                return False
            continue
        if parameter["type"] == "integer":
            if (type(value) not in (int, float) or not math.isfinite(value) or value != int(value)
                    or not parameter["minimum"] <= value <= parameter["maximum"]):
                return False
            if "maximum_from" in parameter:
                other = arguments.get(parameter["maximum_from"])
                if type(other) not in (int, float) or value > other:
                    return False
        elif parameter["type"] == "string":
            if type(value) is not str:
                return False
            length = len(value.strip().encode("utf-16-le")) // 2
            if not parameter.get("min_length", 1) <= length <= parameter.get("max_length", 120):
                return False
            if any(unicodedata.category(c) == "Cc" and c not in "\n\r\t" for c in value):
                return False
            if "enum" in parameter and value not in parameter["enum"]:
                return False
        else:
            return False
    return True


def tool_description(name):
    tool = TOOLS[name]
    parameters = {p["name"]: {k: v for k, v in p.items() if k != "name"} for p in tool["parameters"]}
    return "arguments: " + json.dumps(parameters, ensure_ascii=True, separators=(",", ":")) + ". " + tool["description"]
