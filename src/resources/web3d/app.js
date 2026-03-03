let scene, camera, renderer, controls;
let carBody, wheels = [];

// Initialize Scene
function init() {
    // 1. Scene setup
    scene = new THREE.Scene();
    scene.background = new THREE.Color(0x060810); // Match Java app background
    // Subtle fog to blend into background
    scene.fog = new THREE.Fog(0x060810, 10, 50);

    // 2. Camera setup
    camera = new THREE.PerspectiveCamera(45, window.innerWidth / window.innerHeight, 0.1, 100);
    camera.position.set(5, 3, 5);

    // 3. Renderer setup
    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setSize(window.innerWidth, window.innerHeight);
    renderer.setPixelRatio(window.devicePixelRatio);
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    document.body.appendChild(renderer.domElement);

    // 4. Lighting Setup (Studio finish)
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.6);
    scene.add(ambientLight);

    const mainLight = new THREE.DirectionalLight(0xffffff, 1.2);
    mainLight.position.set(5, 10, 5);
    mainLight.castShadow = true;
    mainLight.shadow.mapSize.width = 2048;
    mainLight.shadow.mapSize.height = 2048;
    scene.add(mainLight);

    const fillLight = new THREE.DirectionalLight(0xaaccff, 0.8);
    fillLight.position.set(-5, 0, -5);
    scene.add(fillLight);

    // 5. Build Placeholder Car Model
    buildPlaceholderCar();

    // 6. Floor grid
    const gridHelper = new THREE.GridHelper(20, 20, 0x00d4ff, 0x222222);
    gridHelper.position.y = 0;
    gridHelper.material.opacity = 0.2;
    gridHelper.material.transparent = true;
    scene.add(gridHelper);

    // Reflective floor plane
    const floorGeo = new THREE.PlaneGeometry(50, 50);
    const floorMat = new THREE.MeshStandardMaterial({
        color: 0x060810,
        roughness: 0.1,
        metalness: 0.8
    });
    const floor = new THREE.Mesh(floorGeo, floorMat);
    floor.rotation.x = -Math.PI / 2;
    floor.receiveShadow = true;
    scene.add(floor);

    // 7. Controls
    controls = new THREE.OrbitControls(camera, renderer.domElement);
    controls.enableDamping = true;
    controls.dampingFactor = 0.05;
    controls.minDistance = 3;
    controls.maxDistance = 15;
    controls.maxPolarAngle = Math.PI / 2 - 0.05; // Prevent dipping below floor

    // 8. Event listener for resize
    window.addEventListener('resize', onWindowResize);

    // Start animation loop
    animate();
}

function buildPlaceholderCar() {
    // Car Body Material implementation (Shiny clearcoat)
    const bodyMat = new THREE.MeshPhysicalMaterial({
        color: 0xdc1e28, // Default Racing Red
        metalness: 0.6,
        roughness: 0.2,
        clearcoat: 1.0,
        clearcoatRoughness: 0.1
    });

    const bodyGeo = new THREE.BoxGeometry(2, 0.6, 4);
    carBody = new THREE.Mesh(bodyGeo, bodyMat);
    carBody.position.y = 0.6;
    carBody.castShadow = true;
    scene.add(carBody);

    // Cabin
    const cabinGeo = new THREE.BoxGeometry(1.6, 0.5, 2);
    // Tinted glass material
    const glassMat = new THREE.MeshPhysicalMaterial({
        color: 0x111111,
        metalness: 0.9,
        roughness: 0.1,
        transmission: 0.2, // glass-like
        transparent: true
    });
    const cabin = new THREE.Mesh(cabinGeo, glassMat);
    cabin.position.y = 0.55;
    cabin.position.z = -0.2;
    cabin.castShadow = true;
    carBody.add(cabin);

    // Wheels setup
    const wheelGeo = new THREE.CylinderGeometry(0.35, 0.35, 0.2, 32);
    // Alloy Material
    const alloyMat = new THREE.MeshStandardMaterial({
        color: 0x888888,
        metalness: 0.9,
        roughness: 0.3
    });

    const wheelPos = [
        [-1.1, 0.35, 1.2],
        [1.1, 0.35, 1.2],
        [-1.1, 0.35, -1.2],
        [1.1, 0.35, -1.2]
    ];

    wheelPos.forEach(pos => {
        const wheel = new THREE.Mesh(wheelGeo, alloyMat);
        wheel.rotation.z = Math.PI / 2;
        wheel.position.set(...pos);
        wheel.castShadow = true;
        wheels.push(wheel);
        scene.add(wheel);
    });
}

// Window resize handler
function onWindowResize() {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
}

// Animation Loop
function animate() {
    requestAnimationFrame(animate);
    // Smooth idle rotation
    scene.rotation.y += 0.001;
    controls.update();
    renderer.render(scene, camera);
}

// ==========================================
// EXPOSED APIs FOR JAVA INTEGRATION
// ==========================================

// Must attach to window object so JavaFX WebEngine can call them

window.changeBodyColor = function (hexColor) {
    if (carBody) {
        // three.js expects hex number or string
        carBody.material.color.set(hexColor);
    }
};

window.toggleRims = function (rimType) {
    let newColor, newMetal, newRough;
    switch (rimType) {
        case "Sport":
        case "Sport Twin-Spoke":
            newColor = 0x333333; newMetal = 0.8; newRough = 0.5; break;
        case "Carbon Fiber":
            newColor = 0x111111; newMetal = 0.4; newRough = 0.7; break;
        case "Forged Alloy":
            newColor = 0xaaaaaa; newMetal = 1.0; newRough = 0.1; break;
        default: // Standard
            newColor = 0x888888; newMetal = 0.9; newRough = 0.3; break;
    }

    wheels.forEach(wheel => {
        wheel.material.color.setHex(newColor);
        wheel.material.metalness = newMetal;
        wheel.material.roughness = newRough;
        wheel.material.needsUpdate = true;
    });
};

window.toggleTint = function (tintType) {
    const glass = carBody.children[0];
    if (glass) {
        if (tintType.includes("Privacy") || tintType.includes("Dark")) {
            glass.material.transmission = 0.0;
            glass.material.color.setHex(0x050505);
        } else if (tintType.includes("Light")) {
            glass.material.transmission = 0.3;
            glass.material.color.setHex(0x222222);
        } else { // Clear
            glass.material.transmission = 0.8;
            glass.material.color.setHex(0x88bbff);
        }
    }
};

// Initialize scene
init();
