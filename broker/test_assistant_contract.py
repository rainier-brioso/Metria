"""Shared conformance vectors used by both native Android and the cloud broker."""
import hashlib
import json
import unittest

from assistant_contract import (AGENT_POLICY, BASE_PROMPT, EXAMPLES, RAW, SOURCE, SOURCE_SHA256,
                                TOOLS, validate_arguments)
from cloud_assistant import GLOBAL_INSTRUCTIONS, _validate_tool, tool_instructions
from provider import ProviderFailure


class SharedAssistantContractTests(unittest.TestCase):
    def test_shared_conformance_vectors_and_host_tool_adapter(self):
        cases = json.loads((SOURCE.parent / "assistant-contract-cases.json").read_text(encoding="utf-8"))
        self.assertEqual(set(TOOLS), {case["name"] for case in cases if case["accepted"]})
        for case in cases:
            with self.subTest(name=case["name"], arguments=case["arguments"]):
                self.assertEqual(case["accepted"], validate_arguments(case["name"], case["arguments"]))
                tool = {"name": case["name"], "arguments": case["arguments"]}
                if case["accepted"]:
                    self.assertEqual(tool, _validate_tool("chat", tool))
                else:
                    with self.assertRaises(ProviderFailure):
                        _validate_tool("chat", tool)

    def test_policy_prompt_contains_shared_rules_descriptions_and_examples(self):
        self.assertIn(BASE_PROMPT, GLOBAL_INSTRUCTIONS)
        self.assertIn(AGENT_POLICY, GLOBAL_INSTRUCTIONS)
        prompt = tool_instructions("chat")
        for tool in TOOLS.values():
            self.assertIn(tool["description"], prompt)
        for example in EXAMPLES:
            self.assertIn(example["member_text"], prompt)
            self.assertTrue(validate_arguments(example["tool"]["name"], example["tool"]["arguments"]))
        self.assertEqual(hashlib.sha256(RAW).hexdigest(), SOURCE_SHA256)
