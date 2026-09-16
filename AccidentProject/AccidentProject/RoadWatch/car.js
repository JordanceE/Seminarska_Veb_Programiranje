export const SCALE_REAL = 10

const dimensions = {
    hatchback: [5.9, 1.7], sedan: [6.2, 1.8], coupe: [5.9, 1.85], wagon: [6.3, 1.85],
    suv: [6.4, 1.95], mpv: [6.6, 1.9], pickup: [7, 1.9], van: [8.1, 2.05],
    truck: [15.7, 2.5], motorcycle: [2.9, .8], bus: [15.7, 2.5]
}

export function createCar(type, state) {
    const [length, width] = dimensions[type] || dimensions.sedan
    return new Car(type, length, width, state)
}

class Car {
     constructor(type, length, width, state) {
        this.vehicleId = createId("Vehicle")

        this.x =
            (state.canvas.width / 2 - state.cameraOffsetX) /
            state.worldScale

        this.y =
            (state.canvas.height / 2 - state.cameraOffsetY) /
            state.worldScale

        this.width = length * SCALE_REAL
        this.height = width * SCALE_REAL

        this.rotation = 0
        this.scale = 1
        this.note = ""
        this.flipped = false
        this.confidence = null

        this.vehicleData = {
            name: `V${state.carCounter++}`,
            model: "",
            type,
            color: "",
            plate: "",
            guilty: false,
            comment: ""
        }
        function createId(prefix) {
    const value = crypto.randomUUID
        ? crypto.randomUUID()
        : `${Date.now()}-${Math.random().toString(16).slice(2)}`

    return `${prefix}:${value}`
}
    }


    draw(ctx, { hovered, selected }) {
        ctx.save(); ctx.translate(this.x, this.y); ctx.rotate(this.rotation); ctx.scale(this.scale, this.scale)
        const w = this.width, h = this.height
        ctx.fillStyle = this.vehicleData.guilty ? "#ff2b2b" : "#111"
        ctx.beginPath(); ctx.moveTo(-w/2,-h/2); ctx.lineTo(w*.35,-h/2); ctx.lineTo(w/2,0); ctx.lineTo(w*.35,h/2); ctx.lineTo(-w/2,h/2); ctx.closePath(); ctx.fill()
        ctx.fillStyle="#333"; ctx.fillRect(-w*.25,-h*.35,w*.45,h*.7)
        ctx.fillStyle="#222"; [[-w/3,-h/2-3],[w/6,-h/2-3],[-w/3,h/2-3],[w/6,h/2-3]].forEach(([x,y])=>ctx.fillRect(x,y,w/6,6))
        ctx.strokeStyle="white"; ctx.lineWidth=2; ctx.beginPath(); ctx.moveTo(0,0); ctx.lineTo(w/2+25,0); ctx.stroke()
        ctx.fillStyle="white"; ctx.beginPath(); ctx.moveTo(w/2+25,0); ctx.lineTo(w/2+15,-6); ctx.lineTo(w/2+15,6); ctx.fill()
        ctx.font="12px Arial"; ctx.fillText(this.vehicleData.name,-10,4)
        if (this.note) { ctx.fillStyle="#ffcc00"; ctx.beginPath(); ctx.arc(0,-h/2-6,4,0,Math.PI*2); ctx.fill() }
        if (hovered || this.vehicleData.guilty) { ctx.strokeStyle=hovered?"#ffaa00":"#ff0000"; ctx.strokeRect(-w/2,-h/2,w,h) }
        if (selected) { ctx.fillStyle="#00e5ff"; ctx.beginPath(); ctx.arc(w/2+20,-h/2-20,16,0,Math.PI*2); ctx.fill(); ctx.fillStyle="black"; ctx.font="16px Arial"; ctx.fillText("↻",w/2+13,-h/2-14) }
        ctx.restore()
    }

    contains(mx, my) {
        const cos=Math.cos(-this.rotation), sin=Math.sin(-this.rotation), dx=mx-this.x, dy=my-this.y
        return Math.abs((dx*cos-dy*sin)/this.scale)<=this.width/2 && Math.abs((dx*sin+dy*cos)/this.scale)<=this.height/2
    }
}
