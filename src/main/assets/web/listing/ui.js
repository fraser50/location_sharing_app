function renderGroups(content, groups) {
    // Clear out any content that is already there
    content.innerHTML = "";

    groups.forEach(function(group) {
        var groupButton = document.createElement("button");
        groupButton.className = "optionButton";

        groupButton.innerText = group.groupname;
        groupButton.onclick = function() {
            createRequest(sharedData.getAPI(), "/groups/" + group.groupid, null, function(req) {
                var rsp = req.target.response;

                var decodedResponse = JSON.parse(rsp);

                if (decodedResponse.status == "success") {
                    renderMembers(content, decodedResponse.members, null);
                }
            });
        };

        content.appendChild(groupButton);
    });
}

function renderMembers(content, members, me) {
    content.innerHTML = "";

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