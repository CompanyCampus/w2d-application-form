var Main = {
  init: function() {
    $("tr").click(function(e) {
      window.location = "/records/" + e.currentTarget.dataset.uuid;
    });
    $("#companyCreation").datepicker({
      dateFormat: "yy-mm-dd",
      changeMonth: true,
      changeYear: true,
      maxDate: 0,
      minDate: "-10Y"
    });
    $("#companyCreation").datepicker("option", "dateFormat", "yy-mm-dd");
  }
};
