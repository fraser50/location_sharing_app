function renderGroups(content, groups) {
    // Clear out any content that is already there
    content.innerHTML = "";

    groups.forEach(function(group) {
        var groupButton = document.createElement("button");
        groupButton.className = "optionButton";

        groupButton.innerText = group.groupname;
        // TODO: Add onclick event

        content.appendChild(groupButton);
    });
}

testgroups = [
    {
        groupid: "group1",
        groupname: "Group One",
        desc: "Decription for Group One"
    },

    {
        groupid: "group2",
        groupname: "Group Two",
        desc: "Decription for Group Two"
    },

    {
        groupid: "group3",
        groupname: "Group Three",
        desc: "Decription for Group Three"
    },
];