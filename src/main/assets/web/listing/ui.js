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

function renderGroupAddOptions(content) {
    content.innerHTML = "";

    createBackButton(content, function() {
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
    joinButton.innerText = "Join Group using Camera";

    content.appendChild(createNewButton);
    content.appendChild(joinButton);
}

function renderCreateGroupForm(content) {
    content.innerHTML = "";

    createBackButton(content, function() {
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

function createAuthForm(content) {
    content.innerHTML = "";

    var heading = document.createElement("h2");
    heading.innerText = "Login";

    var qrCodeSignInButton = document.createElement("button");
    qrCodeSignInButton.className = "optionButton";
    qrCodeSignInButton.innerText = "Sign in using QR code";

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
        sharedData.setAuthKey(authInput.value);

        createRequest(sharedData.getAPI(), "/", null, function(req) {
            var rsp = req.target.response;
            var decodedResponse = JSON.parse(rsp);

            if (decodedResponse.status == "failure") {
                errorMsg.innerHTML = "Login Error:<br>";
                errorMsg.innerHTML += decodedResponse.reason;
                errorMsg.className = "red";

            } else {
                document.getElementById("mapHolder").className = "";
                content.className = "";
                loadGroups(content);
                sharedData.saveAuthKey();
            }

        }, function() {
            errorMsg.innerText = "Unknown Error";
            errorMsg.className = "red";
        });
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