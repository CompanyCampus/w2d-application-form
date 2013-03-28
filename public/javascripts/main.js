var Main = {
  init: function() {
    $("tr").click(function(e) {
      window.location = "/records/" + e.currentTarget.dataset.uuid;
    });
    $("#companyCreation").datepicker();
    $("#companyCreation").datepicker("option", "dateFormat", "yy-mm-dd");
  }
};
