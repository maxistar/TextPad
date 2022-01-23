// Initialize and add the map
function initMap() {
    const elMap = document.getElementById("map");
    // The location of Uluru
    const uluru = { lat: parseFloat(elMap.dataset.latitude), lng: parseFloat(elMap.dataset.longitude) };
    // The map, centered at Uluru
    const map = new google.maps.Map(elMap, {
        zoom: 17,
        center: uluru,
    });
    // The marker, positioned at Uluru
    const marker = new google.maps.Marker({
        position: uluru,
        map: map,
    });
}
