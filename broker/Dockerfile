FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 PORT=8765
WORKDIR /app
COPY broker/ /app/broker/
COPY shared/assistant-contract.json /app/shared/assistant-contract.json
RUN mkdir /state && chown 10001:10001 /state
USER 10001:10001
EXPOSE 8765
CMD ["python", "broker/hosted.py"]
