function generateStatus(name, state) {
    var heading = document.createElement("h2");
    heading.innerText = name + ": ";

    var span = document.createElement("span");
    span.innerText = state ? "Enabled" : "Disabled";
    span.className = state ? "green" : "red";

    heading.appendChild(span);

    return heading;
}

function createBackButton(place, onclick) {
    var backButton = document.createElement("button");
    backButton.className = "backButton";
    backButton.innerText = "Back";
    backButton.onclick = onclick;

    place.appendChild(backButton);
}

function renderSettings(content) {
    content.innerHTML = "";

    createBackButton(content, function() {
        closer.close();
    });

    content.appendChild(generateStatus("Location tracking", settings.isLocationServicesEnabled()));

    var toggleButton = document.createElement("button");
    toggleButton.innerText = "Toggle";
    toggleButton.className = "optionButton";

    toggleButton.onclick = function() {
        settings.setLocationServicesEnabled(!settings.isLocationServicesEnabled());
        renderSettings(content);
    };

    content.appendChild(toggleButton);

    content.appendChild(generateStatus("Extended tracking"), settings.isExtendedTracking());

    var description = document.createElement("h3");
    description.innerText = "Extended tracking will only be enabled if you are participating in study 2. Study 2 involves tracking outside of the HW campus. The first option, location tracking, states whether your location on the campus may be tracked. Turning this off will stop the app from sending location updates to the server.";

    content.appendChild(description);
}