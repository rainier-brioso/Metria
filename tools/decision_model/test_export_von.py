import unittest

import torch

from export_von import _probabilities


class ExportProbabilityShapeTest(unittest.TestCase):
    def test_single_request_probabilities_are_a_flat_vector(self):
        probabilities = _probabilities(torch.tensor([0.0, 1.0, 2.0]), 1.0)

        self.assertEqual(len(probabilities), 3)
        self.assertTrue(all(isinstance(value, float) for value in probabilities))
        self.assertAlmostEqual(sum(probabilities), 1.0, places=6)


if __name__ == "__main__":
    unittest.main()
