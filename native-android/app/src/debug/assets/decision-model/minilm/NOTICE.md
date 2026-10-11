MiniLM debug model asset

The ONNX model and vocabulary are from `sentence-transformers/all-MiniLM-L6-v2`
at Hugging Face revision `1110a243fdf4706b3f48f1d95db1a4f5529b4d41`.
The model is licensed under Apache License 2.0; see `LICENSE-2.0.txt`.
The upstream model card describes 384-dimensional sentence embeddings and
attention-mask-aware mean pooling. This debug router uses frozen text examples
as similarity prototypes plus a separate closed-scope guard. No Metria-trained
classifier is included or claimed.

Upstream model card:
https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/tree/1110a243fdf4706b3f48f1d95db1a4f5529b4d41
