var LAST="last name";
var FIRST="first name";

/**
 * Helper Function to compare users by Last Name
 * @param a
 * @param b
 * @returns {number}
 */
function compareLast(a, b){
	var splitA = a.name.split(", ");
	var splitB = b.name.split(", ");
	var lastA = splitA[0].toUpperCase();
	var lastB = splitB[0].toUpperCase();

	if(lastA < lastB) return -1;
	if(lastA > lastB) return 1;
	
	var firstA = splitA[1].toUpperCase();
	var firstB = splitB[1].toUpperCase();
	if(firstA < firstB) return -1;
	if(firstA > firstB) return 1;
	
	return 0;
}

/**
 * Helper Function to compare users by First Name
 * @param a
 * @param b
 * @returns {number}
 */
function compareFirst(a, b){
	var splitA = a.name.split(", ");
	var splitB = b.name.split(", ");
	var firstA = splitA[1].toUpperCase();
	var firstB = splitB[1].toUpperCase();

	if(firstA < firstB) return -1;
	if(firstA > firstB) return 1;
	
	var lastA = splitA[0].toUpperCase();
	var lastB = splitB[0].toUpperCase();
	if(lastA < lastB) return -1;
	if(lastA > lastB) return 1;
	
	return 0;
}

/**
 * Insert a list of students into the Progress Table
 * @param sortMethod
 */
function getStudents(sortMethod){

	if(students.length > 1){
		if(sortMethod == FIRST) {
			students.sort(compareFirst);
		} else {
			students.sort(compareLast);
		}
	}
	
	//erase existing table cells and rows
	var progressTableBody = document.getElementById("progressTableBody");
	var clone = progressTableBody.cloneNode(false);
	progressTableBody.parentNode.replaceChild(clone, progressTableBody);

	buildProgressTable(students, clone);

	//Adds the modal functionality to each user
	modalJS();
}

/**
 * Reverses the order of the students in the list
 */
function reverseStudentOrder() {
	students.reverse();

	
	//erase existing table cells and rows
	var progressTableBody = document.getElementById("progressTableBody");
	var clone = progressTableBody.cloneNode(false);
	progressTableBody.parentNode.replaceChild(clone, progressTableBody);

	buildProgressTable(students, clone);

	modalJS();
}

/**
 * Builds a progress table with all students
 * @param students
 * @param clone
 */
function buildProgressTable(students, clone){
	students.forEach(function(student){
		var progressPercent = student.progress;

		if(progressPercent == null){
			progressPercent = 0;
		}

		var row = clone.insertRow(-1);
		var name = row.insertCell(0);
		var cell = row.insertCell(1);

		var progressDiv = createProgressBar(progressPercent);

		var link = document.createElement("a");
		link.setAttribute("id", student.id);
		link.setAttribute("class", "open");
		link.setAttribute("href", "#");
		link.innerText = student.name;

		name.appendChild(link);
		name.style.border = "1px solid #eaeaea";

		cell.appendChild(progressDiv);
		cell.style.borderLeft = "1px solid #eaeaea";
		cell.style.borderTop = "1px solid #eaeaea";
		cell.style.borderBottom = "1px solid #eaeaea";
		cell.style.borderRight = "1px solid #bababa";

	});
}

/**
 * Builds the modal for the user specified
 */
function modalJS(){

	//Sets up the window to lock scrolling when the modal is opened
	var $window = $(window), previousScrollTop = 0, scrollLock = false;

	//prevents scrolling if it is locked
	$window.scroll(function(event) {
		if(scrollLock) {
			$window.scrollTop(previousScrollTop);
		}

		previousScrollTop = $window.scrollTop();

	});

	//Opens a modal
	$('.open').click(function(event) {

		//works only when the links on the User's names are clicked
        if($(this).attr("class") == "open"){

        	var topPosition = $('#menu').position().top;

        	//used for mobile devices to prevent common errors with the modal size, movement, and closing
			if('ontouchstart' in window){


				$('#modal').css({
					top: topPosition,
					left: -10,
					height: "auto",
					width: $(window).width(),
					paddingBottom: 20
				});
			}
			else{
				$('#modal').css({
					top: topPosition + 15,
					left: $('#menu').position().left + 15,
					height: $(window).height() * .50,
					width: $(window).width() * .75,
					paddingBottom: 20
				});

				//Locks scrolling
				scrollLock = true;
			}

            var nameText = $(this).text().split(", ");

            $("#modalHeader").text("Progress Percentage for " + nameText[1] + " " + nameText[0]);

            $('#overlay, #modal').show();

            //Calls the server to get a JSON object with all active ProgressSiteConfigurations and their Progress Items
			//for the clicked student
            getStudentProgressDetail(event.target.id);

            event.preventDefault();
        }
	});

	//When the close button is clicked
	$('#close').on('click touchstart', function() {

		$('#modal, #overlay').hide();

		event.preventDefault();

		scrollLock = false;
		return false;

	});

	//Used for when the pointer hovers over the close button
	$('#close').hover(function () {
		$(this).css('color', 'red');
	}, function(){
		$(this).css('color', '#bdbdbd');
	});

	//Allows the modal to be movable on the screen
	$('#modal').draggable({
		scroll: true,
		cursor: "move",
		containment: "body"
	});
}

/**
 * Builds the table that is displayed for the student and on the Modal
 * @param returnVals
 */
function buildModalTable(returnVals){

	if(role == "student"){
		tableDiv = document.getElementById("studentDiv");
	}
	else{
		tableDiv = document.getElementById("tableDiv");
	}

	var clone = tableDiv.cloneNode(false);
	tableDiv.parentNode.replaceChild(clone, tableDiv);

	returnVals.forEach(function(implementation){

		var implInfo = document.createElement("table");

		var row = implInfo.insertRow(-1);

		var name = row.insertCell(0);
		var cell = row.insertCell(1);


		var implName = document.createElement("h2");
		implName.append(document.createTextNode(implementation.implementation));

		name.style.width = "220px";
		name.appendChild(implName);

		var progressDiv = createProgressBar(implementation.percentageComplete, "modal");

		cell.style.width = "102px";
		cell.style.verticalAlign = "middle";
		cell.appendChild(progressDiv);

		implInfo.style.margin = "0px 0px 10px 0px";

		clone.appendChild(implInfo);

		var progressItemsTable = document.createElement("table");
		progressItemsTable.style.width = "100%";

		var progressItemRow = progressItemsTable.insertRow(-1);

		var headerName = progressItemRow.insertCell(0);
		var headerPercentage = progressItemRow.insertCell(1);
		var headerPercentageOfImpl = progressItemRow.insertCell(2);

		headerName.style.fontWeight = "bold";
		headerName.style.width = "33%";

		headerPercentage.style.fontWeight = "bold";
		headerPercentage.style.width = "33%";

		headerPercentageOfImpl.style.fontWeight = "bold";
		headerPercentageOfImpl.style.width = "33%";

		headerName.appendChild(document.createTextNode("Progress Item"));
		headerPercentage.appendChild(document.createTextNode("Percent Complete"));
		headerPercentageOfImpl.appendChild(document.createTextNode("Percentage of Category"));

		headerName.style.border = "1px solid #eaeaea";
		headerPercentage.style.border = "1px solid #eaeaea";
		headerPercentageOfImpl.style.border = "1px solid #eaeaea";

		implementation.progressItems.forEach(function(progressItem){
			var progressRow = progressItemsTable.insertRow(-1);

			var progressItemName = progressRow.insertCell(0);
			var progressItemPercentage = progressRow.insertCell(1);
			var progressItemPercentageofImpl = progressRow.insertCell(2);

			progressItemName.appendChild(document.createTextNode(progressItem.progressItem));
			progressItemPercentage.appendChild(document.createTextNode(progressItem.percentage));
			progressItemPercentageofImpl.appendChild(document.createTextNode(progressItem.percentageImplementation));

			progressItemName.style.border = "1px solid #eaeaea";
			progressItemPercentage.style.border = "1px solid #eaeaea";
			progressItemPercentageofImpl.style.border = "1px solid #eaeaea";

		});

		clone.appendChild(progressItemsTable);

	});

	clone.style.padding = "0px 20px";

	if(role == "student"){
		$("#studentDiv").append(clone);
	}
	else{
		$("#tableDiv").append(clone);
	}
}

/**
 * Creates a Progress Bar
 * @param percentageComplete
 * @param type
 * @returns {HTMLDivElement}
 */
function createProgressBar (percentageComplete, type) {


	var textNode = document.createTextNode(percentageComplete + "%");

	var span = document.createElement("span");
	span.style.color = "#000000";
	span.style.fontWeight = "bold";
	span.appendChild(textNode);

	var progressBar = document.createElement("div");
	progressBar.classList.add("progress-bar");
	progressBar.setAttribute("role", "progressbar");
	progressBar.setAttribute("style", "width: " + percentageComplete + "%; background-color: #00acde");
	progressBar.setAttribute("aria-valuemin", "0");
	progressBar.setAttribute("aria-valuemax", "100");
	progressBar.setAttribute("aria-valuenow", percentageComplete);
	progressBar.appendChild(span);

	var progressDiv = document.createElement("div");
	progressDiv.classList.add("progress");
	progressDiv.appendChild(progressBar);

	if(type == "modal"){
		progressBar.style.verticalAlign = "middle";
		progressBar.style.margin = "0px";
		progressBar.style.padding = "0px";

		progressDiv.style.verticalAlign = "middle";
		progressDiv.style.margin = "0px";
		progressDiv.style.padding = "0px";
	}

	return progressDiv;
}

/**
 * Runs when the window is finished loading
 */
window.onload = function () {

	if(role == "student"){
		getStudentProgressDetail(students[0].id);
		$('#settings').hide();
	}
	else{
		document.getElementById("progressTable").classList.add("fixed_header");
		getStudents(LAST);
	}
}