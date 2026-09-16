FROM python:3.12-slim

WORKDIR /app/accident-ai

# This service performs inference on the CPU. Installing PyTorch from its
# CPU-only package index avoids downloading the very large CUDA toolkit and
# NVIDIA cuDNN packages during every Docker build.
ENV PIP_DEFAULT_TIMEOUT=180 \
    PIP_RETRIES=10 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# Linux libraries required by OpenCV / Ultralytics
RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    libxcb1 \
    libxext6 \
    libsm6 \
    libxrender1 \
    && rm -rf /var/lib/apt/lists/*

COPY accident-ai/requirements.txt .

RUN python -m pip install --no-cache-dir \
        --index-url https://download.pytorch.org/whl/cpu \
        torch torchvision \
    && python -m pip install --no-cache-dir -r requirements.txt

COPY accident-ai/ .
COPY RoadWatch/ /app/RoadWatch/

EXPOSE 5000

CMD ["python", "server.py"]
