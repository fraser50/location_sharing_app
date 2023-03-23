var qrCallback = null;

function readQRCode(callback) {
    qrCallback = callback;
    sharedData.scanQRCode();
}

function processQR() {
    qrCallback(sharedData.getQRValue());
}

function renderGroups(content, groups) {
    // Clear out any content that is already there
    content.innerHTML = "";

    var heading = document.createElement("h2");
    heading.innerText = "Groups";
    content.appendChild(heading);
    
    var createGroupButton = document.createElement("button");
    createGroupButton.innerText = "+";
    createGroupButton.className = "createButton";

    createGroupButton.onclick = function() {
        renderGroupAddOptions(content);
    };

    content.appendChild(createGroupButton);

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
    backButton.innerText = "Back";
    backButton.onclick = onclick;

    place.appendChild(backButton);
}

function renderMembers(content, group, members) {
    console.log(group);
    //addMapMarkers(group.groupid);
    content.innerHTML = "";

    createBackButton(content, function() {
        loadGroups(content);
    });

    var settingsButton = document.createElement("button");
    settingsButton.className = "optionButton";
    settingsButton.innerText = "Settings";

    settingsButton.onclick = function() {
        renderGroupSettings(content, group, members);
    };

    content.appendChild(settingsButton);

    var invitePageButton = document.createElement("button");
    invitePageButton.innerText = "+";
    invitePageButton.className = "createButton";

    invitePageButton.onclick = function() {
        renderInvitePage(content, group.groupid);
    };

    content.appendChild(invitePageButton);

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

function renderInvitePage(content, groupID) {
    content.innerHTML = "";

    createBackButton(content, function() {
        document.getElementById("mapHolder").className = "";
        content.className = "content";
        loadGroups(content);
    });

    document.getElementById("mapHolder").className = "hiddenDiv";
    content.className = "contentFullHeight";

    createRequest(sharedData.getAPI(), "/createinvite/" + groupID, null, function(req) {
        var rsp = req.target.response;

        var decodedResponse = JSON.parse(rsp);

        if (decodedResponse.status == "success") {
            var heading = document.createElement("h2");
            heading.innerText = "Invite Code";
            content.appendChild(heading);
            
            var actualHeading = document.createElement("h2");
            actualHeading.innerText = decodedResponse.inviteID;
            content.appendChild(actualHeading);
        }

    }, function() {});
}

function renderGroupSettings(content, group, members) {
    content.innerHTML = "";

    document.getElementById("mapHolder").className = "hiddenDiv";
    content.className = "contentFullHeight";

    createBackButton(content, function() {
        document.getElementById("mapHolder").className = "";
        content.className = "content";
        renderMembers(content, group, members);
    });

    var leaveButton = document.createElement("button");
    leaveButton.className = "optionButton";
    leaveButton.innerText = "Leave Group";

    leaveButton.onclick = function() {
        createRequest(sharedData.getAPI(), "/leavegroup/" + group.groupid, null, function(req) {
            var rsp = req.target.response;
            var decodedResponse = JSON.parse(rsp);
            
            if (decodedResponse.status == "success") {
                document.getElementById("mapHolder").className = "";
                content.className = "content";
                loadGroups(content);
            }

        }, function() {});
    };

    content.appendChild(leaveButton);
}

function renderGroupAddOptions(content) {
    content.innerHTML = "";
    document.getElementById("mapHolder").className = "hiddenDiv";
    content.className = "contentFullHeight";

    createBackButton(content, function() {
        document.getElementById("mapHolder").className = "";
        content.className = "content";
        loadGroups(content);
    });

    var heading = document.createElement("h2");
    heading.innerText = "Add Group";
    content.appendChild(heading);

    var createNewButton = document.createElement("button");
    createNewButton.className = "optionButton";
    createNewButton.innerText = "Create New Group";

    createNewButton.onclick = function() {
        renderCreateGroupForm(content);
    };

    // This button will be used in the future for opening up the phone camera to scan a QR code from another user
    var joinButton = document.createElement("button");
    joinButton.className = "optionButton";
    joinButton.innerText = "Join Group";

    joinButton.onclick = function() {
        renderJoinGroupForm(content);
    };

    content.appendChild(createNewButton);
    content.appendChild(joinButton);
}

function renderCreateGroupForm(content) {
    content.innerHTML = "";
    document.getElementById("mapHolder").className = "hiddenDiv";
    content.className = "contentFullHeight";

    createBackButton(content, function() {
        document.getElementById("mapHolder").className = "";
        loadGroups(content);
    });

    var groupNameText = document.createElement("p");
    groupNameText.innerText = "Group Name";
    groupNameText.style = "font-size: medium";

    var groupNameInput = document.createElement("input");
    groupNameInput.className = "inputField";
    groupNameInput.type = "text";

    var groupDescriptionText = document.createElement("p");
    groupDescriptionText.innerText = "Group Description";
    groupDescriptionText.style = "font-size: medium";

    var groupDescriptionInput = document.createElement("textarea");
    //groupNameInput.className = "inputField";
    //groupNameInput.type = "text";

    var submitButton = document.createElement("button");
    submitButton.className = "optionButton";
    submitButton.innerText = "Create Group";

    submitButton.onclick = function() {
        createRequest(sharedData.getAPI(), "/creategroup", {
            name: groupNameInput.value,
            desc: groupDescriptionInput.value

        }, function(req) {
            var rsp = req.target.response;
            var decodedResponse = JSON.parse(rsp);

            if (decodedResponse.status == "success") {
                loadGroups(content);

            } else {
                alert("Server-side error");
                loadGroups(content);
            }

        }, function(err) {
            alert("Failed to create group");
            loadGroups(content);
        });
    };

    content.appendChild(groupNameText);
    content.appendChild(groupNameInput);

    content.appendChild(groupDescriptionText);
    content.appendChild(groupDescriptionInput);

    content.appendChild(submitButton);
}

function renderJoinGroupForm(content) {
    content.innerHTML = "";

    createBackButton(content, function() {
        document.getElementById("mapHolder").className = "";
        content.className = "content";
        loadGroups(content);
    });

    var heading = document.createElement("h2");
    heading.innerText = "Join Group";
    content.appendChild(heading);

    var joinCameraButton = document.createElement("button");
    joinCameraButton.className = "optionButton";
    joinCameraButton.innerText = "Join Group using Camera";

    function joinGroup(code) {
        // TODO: Set nickname
        createRequest(sharedData.getAPI(), "/joingroup/" + code, null, function(req) {
            console.log("joinGroup called");
            var rsp = req.target.response;
            var decodedResponse = JSON.parse(rsp);

            if (decodedResponse.status == "success") {
                console.log("Success");
                document.getElementById("mapHolder").className = "";
                content.className = "content";
                loadGroups(content);

            } else {
                console.log("Failure");
                alert("That group code is not valid!");
            }
        }, function() {});
    }

    joinCameraButton.onclick = function() {
        readQRCode(function(code) {
            if (code.startsWith("grouplocations:group_")) {
                joinGroup(code.replace("grouplocations:group_", ""));

            } else {
                alert("That was not a join code!");
            }
        });
    };

    var groupCodeText = document.createElement("p");
    groupCodeText.innerText = "Group Join Code";
    groupCodeText.style = "font-size: medium";

    var groupCodeInput = document.createElement("input");
    groupCodeInput.className = "inputField";
    groupCodeInput.type = "text";

    var joinButton = document.createElement("button");
    joinButton.className = "optionButton";
    joinButton.innerText = "Join";

    joinButton.onclick = function() {
        joinGroup(groupCodeInput.value);
    };

    content.appendChild(joinCameraButton);
    content.appendChild(groupCodeText);
    content.appendChild(groupCodeInput);
    content.appendChild(joinButton);

}

function createAuthForm(content) {
    content.innerHTML = "";

    var heading = document.createElement("h2");
    heading.innerText = "Login";

    function login(key) {
        sharedData.setAuthKey(key);

        createRequest(sharedData.getAPI(), "/", null, function(req) {
            var rsp = req.target.response;
            var decodedResponse = JSON.parse(rsp);

            if (decodedResponse.status == "failure") {
                errorMsg.innerHTML = "Login Error:<br>";
                errorMsg.innerHTML += decodedResponse.reason;
                errorMsg.className = "red";

            } else {
                document.getElementById("mapHolder").className = "";
                content.className = "content";
                loadGroups(content);
                sharedData.saveAuthKey();

                // TODO: Might want to have an onLogin event functiion instead of this

                setInterval(function() {
                    var latitude = sharedData.getLatitude();
                    var longitude = sharedData.getLongitude();

                    //userMarker.setLatLng(L.latLng(latitude, longitude));
                }, 1000);

                addAllMapMarkers();

                setInterval(function() {
                    addAllMapMarkers();
                }, 10000);
            }

        }, function() {
            errorMsg.innerText = "Unknown Error";
            errorMsg.className = "red";
        });
    }

    var qrCodeSignInButton = document.createElement("button");
    qrCodeSignInButton.className = "optionButton";
    qrCodeSignInButton.innerText = "Sign in using QR code";
    qrCodeSignInButton.onclick = function() {
        readQRCode(function(code) {
            if (code.startsWith("grouplocations:key_")) {
                login(code.replace("grouplocations:key_", ""));
            }
        });
    };

    var alternativeHeading = document.createElement("h3");
    alternativeHeading.innerText = "Alternatively, sign in by typing in login code";

    var authText = document.createElement("p");
    authText.innerText = "Login Key";
    authText.style = "font-size: medium";

    var authInput = document.createElement("input");
    authInput.className = "inputFieldFull";
    authInput.type = "text";
    authInput.style = "margin-bottom: 15px;";

    var submitLoginButton = document.createElement("button");
    submitLoginButton.className = "optionButton";
    submitLoginButton.innerText = "Log In";

    var errorMsg = document.createElement("h3");
    errorMsg.className = "hiddenDiv";

    submitLoginButton.onclick = function() {
        login(authInput.value);
    };

    content.appendChild(heading);

    content.appendChild(qrCodeSignInButton);

    content.appendChild(alternativeHeading);

    content.appendChild(authText);
    content.appendChild(authInput);

    content.appendChild(submitLoginButton);
    content.appendChild(errorMsg);
}

// This function creates and sends a request to the server, and uses the provided callbacks for the response
function createRequest(apiHost, endpoint, payload, responseCallback, errorCallback) {
    var req = new XMLHttpRequest();
    req.onerror = errorCallback;
    req.onload = responseCallback;

    sharedData.loadAuthKey();

    req.open(payload == null ? "get" : "post", apiHost + endpoint);

    if (payload != null) {
        req.setRequestHeader("Content-Type", "application/json");
    }

    req.send(payload == null ? null : JSON.stringify(payload));
}

function addMapMarkers(groupid) {
    createRequest(sharedData.getAPI(), "/groups/" + groupid + "/locations", null, function(req) {
        var rsp = req.target.response;
        var decodedResponse = JSON.parse(rsp);
        if (decodedResponse.status == "success") {
            markers.forEach((function(marker) {
                map.removeLayer(marker);
            }));

            markers = [];

            decodedResponse.members.forEach(function(member) {
                var marker = L.marker([member.point.x, member.point.y]);
                marker.addTo(map);
                markers.push(marker);
            });
        }

    }, function() {});
}

//TODO: Reduce code duplication

function addAllMapMarkers() {
    createRequest(sharedData.getAPI(), "/locations", null, function(req) {
        var rsp = req.target.response;
        var decodedResponse = JSON.parse(rsp);
        if (decodedResponse.status == "success") {
            markers.forEach((function(marker) {
                map.removeLayer(marker);
            }));

            markers = [];

            decodedResponse.members.forEach(function(member) {
                var marker = L.marker([member.point.x, member.point.y]);
                marker.addTo(map);
                markers.push(marker);
            });
        }

    }, function() {})
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