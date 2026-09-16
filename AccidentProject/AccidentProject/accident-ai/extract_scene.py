from ultralytics import YOLO
import cv2
import numpy as np
from pydantic import BaseModel, Field
from typing import Optional, List

model = YOLO("yolov8n.pt")

YOLO_VEHICLE_CLASSES = {2: "car", 3: "motorcycle", 5: "bus", 7: "truck"}

TYPE_MAP = {
    2: "sedan",
    3: "motorcycle",
    5: "bus",
    7: "truck"
}


class SceneResult(BaseModel):
    location: dict = Field(
        default_factory=dict
    )

    cars: List[dict] = Field(
        default_factory=list
    )

    measurements: List[dict] = Field(
        default_factory=list
    )

    ai_summary: Optional[str] = None
    confidence: Optional[float] = None


def get_rotation_angle(img, x1, y1, x2, y2):
    """
    Extracts dominant orientation of a vehicle crop using PCA on edge points.
    Returns angle in radians, measured from the positive X axis (canvas convention).
    """
    crop = img[int(y1):int(y2), int(x1):int(x2)]
    if crop.size == 0:
        return 0.0

    gray = cv2.cvtColor(crop, cv2.COLOR_BGR2GRAY)
    edges = cv2.Canny(gray, 50, 150)

    pts = np.column_stack(np.where(edges > 0)) 
    if len(pts) < 10:
        return 0.0

    xy = pts[:,::-1].astype(np.float32)

    _, eigenvectors = cv2.PCACompute(xy, mean=None)

    dx = eigenvectors[0][0]
    dy = eigenvectors[0][1]

    angle = float(np.arctan2(dy, dx))  
    
    if angle < -np.pi / 2:
        angle += np.pi
    elif angle > np.pi / 2:
        angle -= np.pi

    return angle


def extract_scene(image_path):
    img = cv2.imread(image_path)
    if img is None:
        return SceneResult()

    h, w = img.shape[:2]
    results = model(image_path)

    cars = []
    confidences = []

    for r in results:
        for box in r.boxes:
            cls = int(box.cls[0])
            if cls not in YOLO_VEHICLE_CLASSES:
                continue

            conf = float(box.conf[0])
            confidences.append(conf)

            x1, y1, x2, y2 = box.xyxy[0]

            cx = float((x1 + x2) / 2)
            cy = float((y1 + y2) / 2)

            bbox_w = float(x2 - x1)
            bbox_h = float(y2 - y1)

            angle = get_rotation_angle(img, x1, y1, x2, y2)
           
            if bbox_h > bbox_w:
                angle += np.pi / 2
          
            while angle > np.pi:
                angle -= 2 * np.pi
            while angle <= -np.pi:
                angle += 2 * np.pi

            cars.append({
                "x": cx,
                "y": cy,
                "width": bbox_w,
                "height": bbox_h,
                "rotation": angle,
                "confidence": conf,
                "vehicleData": {
                    "name": f"V{len(cars)+1}",
                    "model": "",
                    "type": TYPE_MAP.get(cls, "sedan"),
                    "color": "",
                    "plate": "",
                    "guilty": False,
                    "comment": ""
                },
                "note": ""
            })

    avg_conf = float(np.mean(confidences)) if confidences else 0.0

    return SceneResult(
        location={},
        cars=cars,
        measurements=[],
        ai_summary=f"Detected {len(cars)} vehicle(s).",
        confidence=avg_conf
    )
