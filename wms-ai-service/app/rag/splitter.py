def split_text(text: str, chunk_size: int = 800, overlap: int = 120) -> list[dict]:
    normalized = "\n".join(line.strip() for line in text.splitlines() if line.strip())
    if not normalized:
        return []

    chunks: list[dict] = []
    start = 0
    index = 0
    while start < len(normalized):
        end = min(start + chunk_size, len(normalized))
        content = normalized[start:end].strip()
        if content:
            chunks.append({"chunkIndex": index, "content": content})
            index += 1
        if end == len(normalized):
            break
        start = max(0, end - overlap)
    return chunks
