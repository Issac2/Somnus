Ext.define('somnus.view.system.user.UpdatePwdWindow', {
    extend: 'Ext.window.Window',
    alias: 'widget.updatePwdWindow',
    title: '修改密码',
    width: 320,
    initComponent: function () {
        this.formPanel = Ext.create('Ext.form.Panel', {
            bodyStyle: 'padding: 5px 10px',
            border: false,
            baseCls: 'x-plain',
            defaultType: 'textfield',
            defaults: {
                labelWidth: 75,
                anchor: '100%'
            },
            items: [{
                    fieldLabel: '新密码',
                    name: 'data.pwd',
                    id: 'newPwd',
                    inputType: "password",
                    allowBlank: false
                },
                {
                    fieldLabel: '确认新密码',
                    inputType: "password",
                    name: 'data.loginPwd',
                    vtype: 'password',
                    vtypeText: "两次密码不一致！",
                    initialPassField: 'newPwd',
                    allowBlank: false
                }
            ]
        });
        
        this.buttons = this.buttons || [];
        var me = this;
		var form = this.formPanel.getForm();
        this.buttons.push({
        	text: '修改',
        	handler: function(){
        		if (!form.isValid()) {
        			return;
        		}
        		form.submit({
        			url:app.contextPath + '/base/syuser!doNotNeedSecurity_updateCurrentPwd.action',
        			submitEmptyText: false,
        			waitMsg: '正在修改密码...',
        			success: function (form, action) {
        				var results = Ext.decode(action.response.responseText);
        				if (results.success) {
        					Ext.Msg.show({
        						title: '信息',
        						msg: '修改成功！',
        						buttons: Ext.Msg.OK,
        						icon: Ext.Msg.INFO,
        						fn: function () {
        							me.fireEvent('success');
        							me.close();
        						}
        					});
        				}
        			}
        		});
        	},
        	scope: this
        },{
        	text: '关闭',
        	handler: this.close,
        	scope: this
        });
        
        this.items = [this.formPanel];

        this.callParent();
    }
});