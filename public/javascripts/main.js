var Main = {
  init: function() {
    $("tbody tr").click(function(e) {
      window.location = "/records/" + e.currentTarget.dataset.uuid;
    });
    $("#companyCreation").datepicker({
      dateFormat: "yy-mm-dd",
      changeMonth: true,
      changeYear: true,
      maxDate: 0,
      minDate: "-10Y"
    });
    $("#pitch").keyup(function() {
      $(".pitch-chars-count").text(400 - $(this).val().length);
    })
    if(window.location.pathname.indexOf("/records/") == 0) {
      $("input,textarea").attr("disabled", "disabled");
    }
  }
};
