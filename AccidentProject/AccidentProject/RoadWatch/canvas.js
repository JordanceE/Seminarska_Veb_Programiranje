import { SCALE_REAL } from "./car.js"

const ROAD_BACKGROUNDS = new Set([
    "glavnaulica.png",
    "roundabout-3-exits.png",
    "roundabout-3-exits-2-lanes.png",
    "roundabout-4-way-1-lanes.png",
    "roundabout-4-way-2-lanes.png",
    "t-junction.png",
    "boulevard_2_full_lines.png",
    "boulevard_tree_line.png"
])

export function initCanvas(state,{measurements,notes,ui}){
    let backend = null

function setBackend(value) {
    backend = value
}
    const background=new Image();
    state.background=background
    let animationStarted=false
    let dragging=false,panning=false,panX=0,panY=0,offX=0,offY=0
    const point=e=>{const r=state.canvas.getBoundingClientRect();return{x:(e.clientX-r.left-state.cameraOffsetX)/state.worldScale,y:(e.clientY-r.top-state.cameraOffsetY)/state.worldScale}}
    state.canvas.onmousedown=e=>{if(state.aiMode)return;const p=point(e)
        for(const car of state.selectedCars){const a=car.rotation,x=car.x+(car.width/2+15)*Math.cos(a)-(-car.height/2-15)*Math.sin(a),y=car.y+(car.width/2+15)*Math.sin(a)+(-car.height/2-15)*Math.cos(a);if(Math.hypot(p.x-x,p.y-y)<12){state.selectedCars.forEach(c=>c.rotation+=Math.PI);return}}
        if(state.measureMode&&!state.selectedCar){
            const car=state.cars.find(c=>c.contains(p.x,p.y));ui.pushHistory();
            const measurementId =
                crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`
            state.currentMeasurement=
                {
                     measurementId: `Measurement:${measurementId}`,
                    id:crypto.randomUUID(),
                    fromVehicleId: car?.vehicleId || null,
                    toVehicleId: null,
                    type: car ? "VEHICLE_TO_POINT" : "POINT_TO_POINT",
                     from: car?.vehicleData.name || null,
                    x1:p.x,
                    y1:p.y,
                    x2:null,
                    y2:null,
                    len:null,
                    manual:false
                };
            state.measurements.push(state.currentMeasurement);measurements.updateList();return}
        const car=state.cars.find(c=>c.contains(p.x,p.y));if(car){ui.pushHistory();if(e.ctrlKey)state.selectedCars=state.selectedCars.includes(car)?state.selectedCars.filter(c=>c!==car):[...state.selectedCars,car];else state.selectedCars=[car];state.selectedCar=car;ui.refresh();dragging=true;offX=p.x-car.x;offY=p.y-car.y}else{state.selectedCar=null;state.selectedCars=[];ui.refresh();panning=true;panX=e.clientX;panY=e.clientY}}
    state.canvas.onmousemove=e=>{const p=point(e);state.hoveredCar=state.cars.find(c=>c.contains(p.x,p.y))||null;if(state.currentMeasurement){state.currentMeasurement.x2=p.x;state.currentMeasurement.y2=p.y;return}if(panning){state.cameraOffsetX+=e.clientX-panX;state.cameraOffsetY+=e.clientY-panY;panX=e.clientX;panY=e.clientY}if(dragging&&state.selectedCar){const x=p.x-offX,y=p.y-offY,dx=x-state.selectedCar.x,dy=y-state.selectedCar.y;state.selectedCars.forEach(car => {
    car.x += dx
    car.y += dy
})

measurements.updateForMovedVehicles(
    state.selectedCars
)

state.currentMeasurement = null}}
    state.canvas.onmouseup =
    async () => {
        dragging = false
        panning = false

        const measurement =
            state.currentMeasurement

        state.currentMeasurement = null

        if (!measurement) {
            return
        }

        if (
            measurement.x2 == null ||
            measurement.y2 == null
        ) {
            state.measurements =
                state.measurements.filter(
                    item =>
                        item !== measurement
                )

            measurements.updateList()
            return
        }

        measurement.len =
            Math.hypot(
                measurement.x2 -
                    measurement.x1,
                measurement.y2 -
                    measurement.y1
            ) / SCALE_REAL

        measurements.updateList()

        try {
            if (backend) {
                await backend.addMeasurement(
                    measurement
                )
            }
        } catch (error) {
            state.measurements =
                state.measurements.filter(
                    item =>
                        item !== measurement
                )

            measurements.updateList()

            alert(
                "Unable to add measurement:\n" +
                error.message
            )
        }
    }
    state.canvas.ondblclick=e=>{const p=point(e),car=state.cars.find(c=>c.contains(p.x,p.y));if(car){state.selectedCar=car;ui.refresh();notes.open(car)}}
    state.canvas.onwheel=e=>{e.preventDefault();state.worldScale*=e.deltaY<0?1.1:.9;ui.updateZoom()}
    function animate(){const {ctx,canvas}=state;ctx.setTransform(1,0,0,1,0,0);ctx.clearRect(0,0,canvas.width,canvas.height);ctx.translate(state.cameraOffsetX,state.cameraOffsetY);ctx.scale(state.worldScale,state.worldScale);if(background.complete&&background.naturalWidth>0)ctx.drawImage(background,state.backgroundOffsetX,state.backgroundOffsetY);measurements.draw();state.cars.forEach(c=>c.draw(ctx,{hovered:c===state.hoveredCar,selected:state.selectedCars.includes(c)}));measurements.drawVehicleDistances();notes.drawTooltip();requestAnimationFrame(animate)}
    function fitBackground(){state.canvas.width=state.canvas.parentElement.clientWidth;state.canvas.height=state.canvas.parentElement.clientHeight;const ratio=background.naturalWidth/background.naturalHeight,cr=state.canvas.width/state.canvas.height,w=ratio>cr?state.canvas.width:state.canvas.height*ratio,h=ratio>cr?state.canvas.width/ratio:state.canvas.height;background.naturalDrawWidth=w;background.naturalDrawHeight=h;background.width=w;background.height=h;state.backgroundOffsetX=(state.canvas.width-w)/2;state.backgroundOffsetY=(state.canvas.height-h)/2;notes.updateList();ui.updateZoom();if(!animationStarted){animationStarted=true;animate()}}
    function loadBackground(fileName){const selected=ROAD_BACKGROUNDS.has(fileName)?fileName:"glavnaulica.png";state.locationData.scene_type=selected;background.src=`assets/${selected}`}
    function start(){background.onload=fitBackground;background.onerror=()=>alert("Unable to load the selected road image.");const sceneType=document.getElementById("sceneType");sceneType.value=state.locationData.scene_type;sceneType.onchange=()=>loadBackground(sceneType.value);loadBackground(sceneType.value)}
    return {start,loadBackground, setBackend}
}
