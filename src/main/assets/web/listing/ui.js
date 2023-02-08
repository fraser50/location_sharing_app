function renderGroups(content, groups) {
    // Clear out any content that is already there
    content.innerHTML = "";

    var heading = document.createElement("h2");
    heading.innerText = "Groups";
    content.appendChild(heading);

    groups.forEach(function(group) {
        var groupButton = document.createElement("button");
        groupButton.className = "optionButton";

        groupButton.innerText = group.groupname;
        groupButton.onclick = function() {
            createRequest(sharedData.getAPI(), "/groups/" + group.groupid, null, function(req) {
                var rsp = req.target.response;

                var decodedResponse = JSON.parse(rsp);

                if (decodedResponse.status == "success") {
                    renderMembers(content, group, decodedResponse.members);
                }
            });
        };

        content.appendChild(groupButton);
    });
}

function loadGroups(content) {
    createRequest(sharedData.getAPI(), "/groups", null, function(req) {
        var rsp = req.target.response;

        var decodedResponse = JSON.parse(rsp);

        if (decodedResponse.status == "success") {
            renderGroups(content, decodedResponse.groups);

        } else {
            // TODO: Display error message
        }

    }, function(req) {
        console.log("error");
    });
}

function createBackButton(place, onclick) {
    var backButton = document.createElement("button");
    backButton.className = "backButton";
    backButton.innerText = "Back"
    backButton.onclick = onclick;

    place.appendChild(backButton);
}

function renderMembers(content, group, members) {
    content.innerHTML = "";

    createBackButton(content, function() {
        loadGroups(content);
    });

    var heading = document.createElement("h2");
    heading.innerText = "Members - " + group.groupname;
    content.appendChild(heading);

    members.forEach(function(member) {
        var memberButton = document.createElement("button");
        memberButton.className = "optionButton";

        memberButton.innerText = member.nickname;

        content.appendChild(memberButton);
    });
}

// This function creates and sends a request to the server, and uses the provided callbacks for the response
function createRequest(apiHost, endpoint, payload, responseCallback, errorCallback) {
    var req = new XMLHttpRequest();
    req.onerror = errorCallback;
    req.onload = responseCallback;

    sharedData.loadAuthKey();

    req.open("get", apiHost + endpoint);

    // TODO: Handle post requests

    req.send();
}

testgroups = [
    {
        groupid: "group1",
        groupname: "Group One",
        groupdescription: "Decription for Group One"
    },

    {
        groupid: "group2",
        groupname: "Group Two",
        groupdescription: "Decription for Group Two"
    },

    {
        groupid: "group3",
        groupname: "Group Three",
        groupdescription: "Decription for Group Three"
    },
];