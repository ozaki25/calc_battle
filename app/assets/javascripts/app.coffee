$ ->
  ws = new WebSocket $('body').data 'ws-url'
  ws.onmessage = (event) ->
    message = JSON.parse event.data
    console.log message
    switch message.type
      when 'question'
        first = message.question.first
        second = message.question.second
        $('#question').html """
          <span class="first">#{first}</span> + <span class="second">#{second}</span>
        """
      when 'updateUser'
        user = message.user
        $("#uid_#{user.uid}").empty()
        updateStar(user)
        finishEffect(user.uid) if message.finish
      when 'updateUsers'
        $('#users').empty()
        for user in message.users
          $('#users').append """
          <li id="uid_#{user.uid}" class="list-group-item"></li>
        """
          updateStar(user)
      else
        console.log "[Error] unmatch message: #{message}"

  updateStar = (user) ->
    $("#uid_#{user.uid}").append user.nicName
    unless user.correctCount is 0
      for i in [1..user.correctCount]
        $("#uid_#{user.uid}").append """
          <span class="glyphicon glyphicon-star" aria-hidden="true"></span>
        """
    unless user.correctCount is 5
      for i in [1..(5 - user.correctCount)]
        $("#uid_#{user.uid}").append """
          <span class="glyphicon glyphicon-star-empty" aria-hidden="true"></span>
        """

  finishEffect = (uid) ->
    $('#answer').attr 'disabled', 'disabled'
    $("#uid_#{uid}").addClass 'list-group-item-success'

  $(document).on 'keypress', '#answer', (e) ->
    ENTER = 13
    if e.which is ENTER
      trmVal = $(this).val().trim()
      return unless trmVal
      input = parseInt trmVal
      first = parseInt $('#question .first').text()
      second = parseInt $('#question .second').text()
      ws.send JSON.stringify { answer: { first: first, second: second, input: input } }
      $(this).val ''

  $('#start').click ->
    name = $('#name').val().trim()
    return unless name
    console.log "your name is #{name}"
    $('#nicname').text name
    $('#answer').removeClass 'hide'
    ws.send JSON.stringify { name: name }
