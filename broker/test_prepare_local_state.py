"""Ownership repair is bounded to directories and cannot replace quota files."""
from pathlib import Path
from types import SimpleNamespace
import stat
import unittest
from unittest.mock import call, patch

import prepare_local_state as preparation


class LocalStatePreparationTests(unittest.TestCase):
    def test_changes_only_known_directory_ownership_using_open_descriptors(self):
        # Windows unit runners do not expose the Linux-only directory open flags.
        with patch.object(Path, "is_symlink", return_value=False), \
                patch.object(Path, "is_dir", return_value=True), \
                patch.object(Path, "lstat", return_value=SimpleNamespace(st_mode=stat.S_IFDIR, st_uid=0, st_gid=0)), \
                patch.object(preparation.os, "O_DIRECTORY", 0x10000, create=True), \
                patch.object(preparation.os, "O_NOFOLLOW", 0x20000, create=True), \
                patch.object(preparation.os, "open", side_effect=[10, 11]) as opened, \
                patch.object(preparation.os, "fstat", return_value=SimpleNamespace(st_mode=stat.S_IFDIR)), \
                patch.object(preparation.os, "fchown", create=True) as ownership, \
                patch.object(preparation.os, "close") as closed:
            preparation.prepare()
        self.assertEqual([call(10, 10001, 10001), call(11, 10001, 10001)], ownership.call_args_list)
        self.assertEqual(["nebius-app-test", "nebius-debug-responses"],
                         [args.args[0].name for args in opened.call_args_list])
        self.assertTrue(all(args.args[1] & 0x20000 for args in opened.call_args_list))
        self.assertEqual([call(10), call(11)], closed.call_args_list)

    def test_refuses_arbitrary_root_and_linked_root(self):
        with self.assertRaises(ValueError):
            preparation.prepare(Path("/other"))
        with patch.object(Path, "is_symlink", return_value=True), self.assertRaises(ValueError):
            preparation.prepare()
